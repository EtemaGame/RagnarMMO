package com.etema.ragnarmmo.skill.runtime;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Central handler for skill effects.
 * Migrated from hardcoded List&lt;ISkillEffect&gt; to SkillRegistry-based
 * lookups.
 *
 * All effects are now registered via SkillDataLoader and stored in
 * SkillRegistry.
 * This class only orchestrates event routing and skill execution logic.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SkillEffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectHandler.class);

    // Convenience: ResourceLocation for First Aid (used in food healing bonus)
    @SuppressWarnings("removal")
    private static final ResourceLocation FIRST_AID_ID = new ResourceLocation("ragnarmmo", "first_aid");

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            handleOffensiveSkills(event, player);
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // Interrupt casting on damage
            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                if (skills instanceof SkillManager && ((SkillManager) skills).isCasting()) {
                    SkillManager sm = (SkillManager) skills;
                    // Check if the skill is interruptible
                    ResourceLocation castingId = sm.getActiveCastSkillId();
                    boolean interruptible = SkillRegistry.get(castingId)
                            .map(ISkillDefinition::isInterruptible)
                            .orElse(true);

                    if (interruptible) {
                        sm.interruptCast();
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("message.ragnarmmo.cast_interrupted")
                                        .withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
            });
            handleDefensiveSkills(event, player);
        }
    }

    private static void handleOffensiveSkills(LivingHurtEvent event, ServerPlayer player) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            var activeSkills = com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.getSkillsForTrigger(ISkillEffect.TriggerType.OFFENSIVE_HURT);
            for (ResourceLocation skillId : activeSkills) {
                int level = skills.getSkillLevel(skillId);
                if (level > 0) {
                    SkillRegistry.getEffect(skillId).ifPresent(effect -> effect.onOffensiveHurt(event, player, level));
                }
            }
        });
    }

    private static void handleDefensiveSkills(LivingHurtEvent event, ServerPlayer player) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            var activeSkills = com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.getSkillsForTrigger(ISkillEffect.TriggerType.DEFENSIVE_HURT);
            for (ResourceLocation skillId : activeSkills) {
                int level = skills.getSkillLevel(skillId);
                if (level > 0) {
                    SkillRegistry.getEffect(skillId).ifPresent(effect -> effect.onDefensiveHurt(event, player, level));
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
            return;

        if (event.player instanceof ServerPlayer serverPlayer) {
            // Casting Tick (Every tick)
            PlayerSkillsProvider.get(serverPlayer).ifPresent(skills -> {
                if (skills instanceof SkillManager && ((SkillManager) skills).tickCast()) {
                    SkillManager sm = (SkillManager) skills;
                    ResourceLocation skillId = sm.getActiveCastSkillId();
                    if (skillId != null) {
                        int level = skills.getSkillLevel(skillId);
                        onCastComplete(serverPlayer, skillId, level);
                        sm.clearCast();
                    }
                }
            });

            if ((event.player.tickCount % 20) != 0)
                return; // Every 1 second (20t)

            PlayerSkillsProvider.get(serverPlayer).ifPresent(skills -> {
                var activeSkills = com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.getSkillsForTrigger(ISkillEffect.TriggerType.PERIODIC_TICK);
                for (ResourceLocation skillId : activeSkills) {
                    int level = skills.getSkillLevel(skillId);
                    if (level > 0) {
                        SkillRegistry.getEffect(skillId)
                                .ifPresent(effect -> effect.onPeriodicTick(event, serverPlayer, level));
                    }
                }
            });

            // Resource regeneration (every 1 second)
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(serverPlayer).ifPresent(s -> {
                if (s instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps) {
                    var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(ps.getJobId());
                    if (job.isMagical()) {
                        double intStat = ps.getINT();
                        double regen = 1.0 + intStat / 10.0;
                        ps.addMana(regen);
                    } else {
                        double vitStat = ps.getVIT();
                        double regen = 2.0 + vitStat / 10.0;
                        ps.addSP(regen);
                    }
                }
            });
        }
    }

    /**
     * Food-based healing bonuses (e.g. First Aid's "bonus healing from food")
     * should not be applied from hurt events.
     * Hook item-use completion instead.
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            var activeSkills = com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.getSkillsForTrigger(ISkillEffect.TriggerType.ITEM_USE_FINISH);
            for (ResourceLocation skillId : activeSkills) {
                int level = skills.getSkillLevel(skillId);
                if (level > 0) {
                    SkillRegistry.getEffect(skillId).ifPresent(effect -> effect.onItemUseFinish(event, player, level));
                }
            }
        });
    }

    /**
     * Re-applies all passive skill effects (attributes, etc.) immediately.
     * Useful on player login/respawn to ensure stats are correct without waiting
     * for the next tick.
     */
    public static void refreshPassiveEffects(ServerPlayer player) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            for (ResourceLocation skillId : SkillRegistry.getAllIds()) {
                int level = skills.getSkillLevel(skillId);
                SkillRegistry.getEffect(skillId).ifPresent(effect -> effect.onPeriodicTick(null, player, level));
            }
        });
    }

    /**
     * Tries to use an active skill.
     * Checks: Level, UsageType, Cooldown, Resource Cost, Cast Time.
     * Uses SkillRegistry for all definitions and effects.
     */
    public static void tryUseSkill(ServerPlayer player, String skillId) {
        // Resolve to ResourceLocation with legacy fallback
        ResourceLocation id;
        if (skillId.contains(":")) {
            id = ResourceLocation.tryParse(skillId);
        } else {
            id = new ResourceLocation("ragnarmmo", skillId.toLowerCase());
        }

        if (id == null)
            return;

        Optional<SkillDefinition> defOpt = SkillRegistry.get(id);
        if (defOpt.isEmpty())
            return;

        SkillDefinition def = defOpt.get();
        if (!def.isActive())
            return;

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int level = skills.getSkillLevel(id);
            if (level <= 0) {
                return;
            }

            if (skills instanceof SkillManager) {
                SkillManager sm = (SkillManager) skills;
                if (sm.isOnCooldown(id)) {
                    return;
                }

                if (sm.isOnGlobalCooldown()) {
                    return;
                }

                if (sm.isCasting()) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component
                                    .translatable("message.ragnarmmo.already_casting")
                                    .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
            }

            // Check resource cost and job permissions
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                // Job-based validation
                var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(stats.getJobId());
                java.util.Set<String> allowedJobs = def.getAllowedJobs();
                
                if (!allowedJobs.isEmpty()) {
                    String currentJobId = job.getId().toUpperCase(java.util.Locale.ROOT);
                    var firstClass = job.getFirstClassAncestor();
                    String ancestorId = firstClass != null ? firstClass.getId().toUpperCase(java.util.Locale.ROOT) : "";

                    boolean jobAllowed = allowedJobs.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(currentJobId) || s.equalsIgnoreCase(ancestorId) 
                                     || s.equalsIgnoreCase("NOVICE") || def.getTier() == com.etema.ragnarmmo.skill.api.SkillTier.LIFE);

                    if (!jobAllowed) {
                        return;
                    }
                }

                int cost = def.getResourceCost(level);

                // Get cast time from definition (preferred) or effect fallback
                int castTime = def.getCastTimeTicks();
                if (castTime == 0) {
                    Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(id);
                    if (effectOpt.isPresent()) {
                        castTime = effectOpt.get().getCastTime(level);
                    }
                }

                // Check if enough resource
                boolean hasEnough = job.isMagical() ? stats.getMana() >= cost : stats.getSP() >= cost;

                if (hasEnough) {
                    if (castTime > 0 && skills instanceof SkillManager) {
                        SkillManager smCast = (SkillManager) skills;
                        // Start Casting — resource consumed on completion
                        smCast.startCast(id, castTime);
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("message.ragnarmmo.cast_start",
                                                def.getDisplayName()));
                    } else {
                        // Instant Cast
                        if (stats.consumeResource(cost)) {
                            executeSkillEffect(player, id, level);
                            if (skills instanceof SkillManager) {
                                SkillManager smCast = (SkillManager) skills;
                                smCast.setCooldown(id, def.getCooldownTicks());
                                if (def.getCastDelayTicks() > 0) {
                                    smCast.setGlobalCooldown(def.getCastDelayTicks());
                                }
                            }
                            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                                    new com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket(
                                            stats));
                        }
                    }
                } else {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component
                                    .translatable("message.ragnarmmo.no_resource")
                                    .withStyle(net.minecraft.ChatFormatting.RED));
                }
            });
        });
    }

    private static void onCastComplete(ServerPlayer player, ResourceLocation skillId, int level) {
        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
            int cost = defOpt.map(d -> d.getResourceCost(level))
                    .orElse(0);

            if (stats.consumeResource(cost)) {
                executeSkillEffect(player, skillId, level);
                PlayerSkillsProvider.get(player).ifPresent(skills -> {
                    if (skills instanceof SkillManager) {
                        SkillManager sm = (SkillManager) skills;
                        int cooldown = defOpt.map(ISkillDefinition::getCooldownTicks).orElse(20);
                        int castDelay = defOpt.map(ISkillDefinition::getCastDelayTicks).orElse(0);

                        sm.setCooldown(skillId, cooldown);
                        if (castDelay > 0) {
                            sm.setGlobalCooldown(castDelay);
                        }
                    }
                });
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                        new com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket(stats));
            }
 else {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component
                                .translatable("message.ragnarmmo.no_resource")
                                .withStyle(net.minecraft.ChatFormatting.RED));
            }
        });
    }

    private static boolean executeSkillEffect(ServerPlayer player, ResourceLocation skillId,
            int level) {
        Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(skillId);
        if (effectOpt.isEmpty()) {
            LOGGER.warn("No effect registered for skill: {}", skillId);
            return false;
        }

        try {
            effectOpt.get().execute(player, level);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error executing skill effect for {}: {}", skillId, e.getMessage());
            return false;
        }
    }
}
