package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMobStats.MOD_ID)
public final class MobCombatHandler {

    private MobCombatHandler() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide())
            return;

        // Player-vs-anything is handled exclusively by CommonEvents.onHurt()
        // which uses full RO combat formulas. Skip here to prevent stacking.
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player)
            return;

        // Prevent double damage processing using centralized guard
        if (DamageProcessingGuard.isProcessedMob(target))
            return;

        float amount = event.getAmount();
        boolean modified = false;

        Entity src = event.getSource().getEntity();
        if (src instanceof LivingEntity attacker) {
            com.etema.ragnarmmo.common.api.mobs.combat.MobCombatViewResolver.resolve(attacker).ifPresent(view -> {
                com.etema.ragnarmmo.system.stats.compute.CombatMath.TargetStats attackerStats = com.etema.ragnarmmo.system.stats.compute.CombatMath.getTargetStats(attacker);
                double mult = 1.0D
                        + attackerStats.str * MobStatsConfigAccess.getDamagePerStr()
                        + attackerStats.dex * MobStatsConfigAccess.getDamagePerDex();
                mult *= view.damageMultiplier();
                
                float finalAmount = (float) Math.max(0.0D, event.getAmount() * mult);

                if (!isMagicDamage(event.getSource())) {
                    var critChance = com.etema.ragnarmmo.system.stats.compute.CombatMath.tryGetResolvedMobCritChance(attacker);
                    if (critChance.isPresent() && com.etema.ragnarmmo.system.stats.compute.CombatMath.rollCritical(critChance.getAsDouble(), attacker.getRandom())) {
                        finalAmount = (float) Math.max(
                                0.0D,
                                finalAmount * com.etema.ragnarmmo.system.stats.compute.CombatMath.computeCritDamageMultiplier(attackerStats.luk, attackerStats.str));
                    }
                }
                
                event.setAmount(finalAmount);
                DamageProcessingGuard.markProcessedMob(target); // Temporary mark to avoid re-processing in this step
            });
        }

        com.etema.ragnarmmo.common.api.mobs.combat.MobCombatViewResolver.resolve(target).ifPresent(view -> {
            com.etema.ragnarmmo.system.stats.compute.CombatMath.TargetStats targetStats = com.etema.ragnarmmo.system.stats.compute.CombatMath.getTargetStats(target);
            double reduction = targetStats.vit * MobStatsConfigAccess.getReductionPerVit();
            double mult = Math.max(0.0D, 1.0D - reduction * view.defenseMultiplier());
            event.setAmount((float) Math.max(0.0D, event.getAmount() * mult));
        });

        if (DamageProcessingGuard.isProcessedMob(target)) {
            // If we mark it processed above, we should ensure the event reflects it.
            // But wait, the original logic had a 'modified' flag.
        }
    }

    private static boolean usesMobCombatScaling(LivingEntity entity) {
        return com.etema.ragnarmmo.common.api.mobs.combat.MobCombatViewResolver.resolve(entity).isPresent();
    }

    private static double resolveDamageMultiplier(MobStats legacyStats) {
        if (legacyStats != null && legacyStats.isInitialized()) {
            return Math.max(0.0D, legacyStats.getDamageMultiplier());
        }
        return 1.0D;
    }

    private static double resolveDefenseMultiplier(MobStats legacyStats) {
        if (legacyStats != null && legacyStats.isInitialized()) {
            return Math.max(0.0D, legacyStats.getDefenseMultiplier());
        }
        return 1.0D;
    }

    private static boolean isMagicDamage(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return true;
        }
        if (source.typeHolder().is(new net.minecraft.resources.ResourceLocation("ragnarmmo", "is_magic"))) {
            return true;
        }
        String msgId = source.getMsgId();
        return msgId.equals("magic") || msgId.equals("indirectMagic");
    }
}
