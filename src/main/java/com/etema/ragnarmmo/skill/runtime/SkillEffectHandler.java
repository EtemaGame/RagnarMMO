package com.etema.ragnarmmo.skill.runtime;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.player.RoPlayerDataAccess;
import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Central handler for skill effects.
 * Migrated from hardcoded List<ISkillEffect> to SkillRegistry-based lookups.
 * All effects are now registered via SkillDataLoader and stored in SkillRegistry.
 * This class only orchestrates event routing and skill execution logic.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SkillEffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectHandler.class);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient())
            return;
        if (event.phase != TickEvent.Phase.START)
            return;

        ServerPlayer player = (ServerPlayer) event.player;
        if (player.level().isClientSide())
            return;

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            ResourceLocation castSkillId = skills.getActiveCastSkillId();
            if (castSkillId != null && shouldInterruptCast(player, castSkillId)) {
                skills.interruptCast();
                player.sendSystemMessage(Component.translatable("message.ragnarmmo.cast_interrupted")
                        .withStyle(ChatFormatting.RED));
            }

            if (skills.tickCast()) {
                ResourceLocation finishedSkillId = skills.getActiveCastSkillId();
                int castLevel = skills.getActiveCastLevel();
                skills.clearCast();

                if (finishedSkillId != null && castLevel > 0) {
                    onCastComplete(player, finishedSkillId, castLevel);
                }
            }
        });

        dispatchTriggeredSkills(player, ISkillEffect.TriggerType.PERIODIC_TICK,
                (effect, level) -> effect.onPeriodicTick(event, player, level));
    }

    @SubscribeEvent
    public static void onOffensiveHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (player.level().isClientSide())
            return;

        dispatchTriggeredSkills(player, ISkillEffect.TriggerType.OFFENSIVE_HURT,
                (effect, level) -> effect.onOffensiveHurt(event, player, level));
    }

    @SubscribeEvent
    public static void onDefensiveHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (player.level().isClientSide())
            return;

        dispatchTriggeredSkills(player, ISkillEffect.TriggerType.DEFENSIVE_HURT,
                (effect, level) -> effect.onDefensiveHurt(event, player, level));
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (player.level().isClientSide())
            return;

        dispatchTriggeredSkills(player, ISkillEffect.TriggerType.ITEM_USE_FINISH,
                (effect, level) -> effect.onItemUseFinish(event, player, level));
    }

    /**
     * Attempts to trigger an active skill use.
     * Delegates to RagnarCombatEngine for authoritative resolution.
     */
    public static void tryUseSkill(ServerPlayer player, String skillId, int level) {
        Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
        if (defOpt.isPresent()) {
            SkillDefinition def = defOpt.get();
            ResourceLocation id = def.getId();

            RoPlayerDataAccess.get(player).ifPresent(data -> {
                IPlayerStats stats = data.getStats();
                SkillManager skills = (SkillManager) data.getSkills();
                if (skills.getSkillLevel(id) < level) {
                    return;
                }

                // Check job requirement
                if (!def.getAllowedJobs().isEmpty()) {
                    String currentJobId = stats.getJobId();
                    com.etema.ragnarmmo.common.api.jobs.JobType jobType =
                            com.etema.ragnarmmo.common.api.jobs.JobType.fromId(currentJobId);

                    java.util.Set<String> allowedJobs = def.getAllowedJobs();
                    boolean jobAllowed = allowedJobs.stream()
                            .anyMatch(jobType::matchesSkillRule)
                            || def.getTier() == com.etema.ragnarmmo.skill.api.SkillTier.LIFE;

                    if (!jobAllowed) {
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.ragnarmmo.skill_wrong_job"));
                        return;
                    }
                }

                if (skills.isCasting() || skills.isOnGlobalCooldown()) {
                    return;
                }

                if (skills.isOnCooldown(id)) {
                    return;
                }

                Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(id);
                int cost = resolveResourceCost(def, effectOpt, level);
                int baseCastTime = resolveBaseCastTime(def, effectOpt, level);
                int castTime = adjustCastTime(player, baseCastTime);
                com.etema.ragnarmmo.skill.api.ResourceType resType = resolveEffectiveResourceType(stats, def);
                boolean hasEnough = resType == com.etema.ragnarmmo.skill.api.ResourceType.COOLDOWN_ONLY
                        || stats.getCurrentResource() >= cost;

                if (hasEnough) {
                    if (castTime > 0) {
                        skills.startCast(id, level, castTime);
                        consumeCastTimeModifiers(player, baseCastTime);
                        player.sendSystemMessage(
                                Component.translatable("message.ragnarmmo.cast_start",
                                        def.getDisplayName()));
                    } else {
                        if (tryConsumeAndExecute(player, stats, def, level, cost)) {
                            skills.setCooldown(id, def.getCooldownTicks(level));
                            int castDelay = resolveCastDelay(def, effectOpt, level, player);
                            if (castDelay > 0) {
                                skills.setGlobalCooldown(castDelay);
                            }

                            syncPlayer(player, stats, skills);
                        }
                    }
                } else {
                    String msg = getInsufficientResourceMessage(resType);
                    player.sendSystemMessage(
                            Component.translatable(msg)
                                    .withStyle(ChatFormatting.RED));
                }
            });
        }
    }

    public static void tryUseSkill(ServerPlayer player, String skillId) {
        Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
        if (defOpt.isEmpty()) {
            return;
        }

        ResourceLocation id = defOpt.get().getId();
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int level = skills.getSkillLevel(id);
            if (level > 0) {
                tryUseSkill(player, skillId, level);
            }
        });
    }

    public static void onCastComplete(ServerPlayer player, ResourceLocation skillId, int level) {
        if (player.level().isClientSide())
            return;

        Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
        var playerDataOpt = RoPlayerDataAccess.get(player);

        if (defOpt.isPresent() && playerDataOpt.isPresent()) {
            SkillDefinition def = defOpt.get();
            var data = playerDataOpt.get();
            IPlayerStats stats = data.getStats();
            SkillManager skills = (SkillManager) data.getSkills();
            Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(skillId);
            int cost = resolveResourceCost(def, effectOpt, level);

            if (tryConsumeAndExecute(player, stats, def, level, cost)) {
                skills.setCooldown(skillId, def.getCooldownTicks(level));
                int castDelay = resolveCastDelay(def, effectOpt, level, player);
                if (castDelay > 0) {
                    skills.setGlobalCooldown(castDelay);
                }

                syncPlayer(player, stats, skills);
            }
        }
    }

    private static void syncPlayer(ServerPlayer player, IPlayerStats stats, SkillManager skills) {
        PlayerStatsSyncService.sync(player, stats, RoPlayerSyncDomain.allMask());
        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(skills.serializeNBT()));
    }

    private static boolean executeSkillEffect(ServerPlayer player, ResourceLocation skillId, int level) {
        Optional<ISkillEffect> effect = SkillRegistry.getEffect(skillId);
        if (effect.isPresent()) {
            try {
                effect.get().execute(player, level);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to execute skill effect for {}: {}", skillId, e.getMessage());
                e.printStackTrace();
            }
        } else {
            LOGGER.warn("No skill effect implementation found for {}", skillId);
        }
        return false;
    }

    public static void refreshPassiveEffects(ServerPlayer player) {
        dispatchTriggeredSkills(player, ISkillEffect.TriggerType.PERIODIC_TICK,
                (effect, level) -> effect.onPeriodicTick(null, player, level));
    }

    private static void dispatchTriggeredSkills(ServerPlayer player, ISkillEffect.TriggerType trigger,
            BiConsumer<ISkillEffect, Integer> action) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            for (ResourceLocation skillId : SkillTriggerRegistry.getSkillsForTrigger(trigger)) {
                int level = skills.getSkillLevel(skillId);
                if (level <= 0) {
                    continue;
                }

                SkillRegistry.getEffect(skillId).ifPresent(effect -> action.accept(effect, level));
            }
        });
    }

    private static int resolveResourceCost(SkillDefinition def, Optional<ISkillEffect> effectOpt, int level) {
        int cost = def.getResourceCost(level);
        if (effectOpt.isPresent()) {
            cost = effectOpt.get().getResourceCost(level, cost);
        }
        return Math.max(0, cost);
    }

    private static int resolveBaseCastTime(SkillDefinition def, Optional<ISkillEffect> effectOpt, int level) {
        int castTime = def.getCastTimeTicks(level);
        if (castTime == 0 && effectOpt.isPresent()) {
            castTime = effectOpt.get().getCastTime(level);
        }
        return Math.max(0, castTime);
    }

    private static int adjustCastTime(ServerPlayer player, int baseCastTicks) {
        if (baseCastTicks <= 0) {
            return 0;
        }

        int dex = (int) Math.round(StatAttributes.getTotal(player, StatKeys.DEX));
        int intel = (int) Math.round(StatAttributes.getTotal(player, StatKeys.INT));
        double adjustedSeconds = CombatMath.computeCastTime(baseCastTicks / 20.0, dex, intel, false);

        MobEffectInstance suffragium = player.getEffect(RagnarMobEffects.SUFFRAGIUM.get());
        if (suffragium != null) {
            double bonusReduction = 0.10 + ((suffragium.getAmplifier() + 1) * 0.05);
            adjustedSeconds *= Math.max(0.20, 1.0 - Math.min(0.50, bonusReduction));
        }

        return Math.max(0, (int) Math.round(adjustedSeconds * 20.0));
    }

    private static void consumeCastTimeModifiers(ServerPlayer player, int baseCastTime) {
        if (baseCastTime > 0 && player.hasEffect(RagnarMobEffects.SUFFRAGIUM.get())) {
            player.removeEffect(RagnarMobEffects.SUFFRAGIUM.get());
        }
    }

    private static int resolveCastDelay(SkillDefinition def, Optional<ISkillEffect> effectOpt, int level, ServerPlayer player) {
        int castDelay = def.getCastDelayTicks(level);
        if (effectOpt.isPresent()) {
            int scalingDelay = effectOpt.get().getCastDelay(level);
            if (scalingDelay > 0) {
                castDelay = scalingDelay;
            }
        }

        // Enforce a 10-tick baseline for ACTIVE skills lacking a configured delay
        if (castDelay == 0 && def.getUsageType() == com.etema.ragnarmmo.skill.api.SkillUsageType.ACTIVE) {
            castDelay = 10;
        }

        return CombatMath.computeCastDelay(castDelay, player);
    }

    private static boolean tryConsumeAndExecute(ServerPlayer player, IPlayerStats stats, SkillDefinition def,
            int level, int cost) {
        com.etema.ragnarmmo.skill.api.ResourceType resType = resolveEffectiveResourceType(stats, def);
        boolean consumed = resType == com.etema.ragnarmmo.skill.api.ResourceType.COOLDOWN_ONLY
                || stats.consumeResource(cost);

        if (!consumed) {
            String msg = getInsufficientResourceMessage(resType);
            player.sendSystemMessage(Component.translatable(msg)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (executeSkillEffect(player, def.getId(), level)) {
            return true;
        }

        if (resType != com.etema.ragnarmmo.skill.api.ResourceType.COOLDOWN_ONLY) {
            stats.addResource(cost);
        }
        return false;
    }

    private static com.etema.ragnarmmo.skill.api.ResourceType resolveEffectiveResourceType(IPlayerStats stats,
            SkillDefinition def) {
        com.etema.ragnarmmo.skill.api.ResourceType declared = def.getResourceType();
        if (declared == com.etema.ragnarmmo.skill.api.ResourceType.COOLDOWN_ONLY) {
            return declared;
        }
        // SP is the unified resource — isMagical() distinction removed in Fase 2
        return com.etema.ragnarmmo.skill.api.ResourceType.SP;
    }

    private static String getInsufficientResourceMessage(com.etema.ragnarmmo.skill.api.ResourceType resourceType) {
        return "message.ragnarmmo.insufficient_sp";
    }

    private static boolean shouldInterruptCast(ServerPlayer player, ResourceLocation skillId) {
        Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
        if (defOpt.isEmpty()) {
            return false;
        }

        Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(skillId);
        boolean interruptible = defOpt.get().isInterruptible()
                && effectOpt.map(ISkillEffect::isInterruptible).orElse(true);
        if (!interruptible) {
            return false;
        }

        return player.hurtTime > 0 || player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
    }
}
