package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
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
            MobStats atk = MobStatsProvider.get(attacker).orElse(null);
            if (usesMobCombatScaling(attacker, atk)) {
                CombatMath.TargetStats attackerStats = CombatMath.getTargetStats(attacker);
                double mult = 1.0D
                        + attackerStats.str * MobConfig.DAMAGE_PER_STR_POINT.get()
                        + attackerStats.dex * MobConfig.DAMAGE_PER_DEX_POINT.get();
                mult *= resolveDamageMultiplier(atk);
                amount = (float) Math.max(0.0D, amount * mult);

                if (!isMagicDamage(event.getSource())) {
                    var critChance = CombatMath.tryGetResolvedMobCritChance(attacker);
                    if (critChance.isPresent() && CombatMath.rollCritical(critChance.getAsDouble(), attacker.getRandom())) {
                        amount = (float) Math.max(
                                0.0D,
                                amount * CombatMath.computeCritDamageMultiplier(attackerStats.luk, attackerStats.str));
                    }
                }

                modified = true;
            }
        }

        MobStats def = MobStatsProvider.get(target).orElse(null);
        if (usesMobCombatScaling(target, def)) {
            CombatMath.TargetStats targetStats = CombatMath.getTargetStats(target);
            double reduction = targetStats.vit * MobConfig.DAMAGE_REDUCTION_PER_VIT_POINT.get();
            double mult = Math.max(0.0D, 1.0D - reduction * resolveDefenseMultiplier(def));
            amount = (float) Math.max(0.0D, amount * mult);
            modified = true;
        }

        if (modified) {
            event.setAmount(amount);
            DamageProcessingGuard.markProcessedMob(target);
        }
    }

    private static boolean usesMobCombatScaling(LivingEntity entity, MobStats legacyStats) {
        return ManualMobProfileRuntimeStore.get(entity).isPresent()
                || (legacyStats != null && legacyStats.isInitialized());
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
