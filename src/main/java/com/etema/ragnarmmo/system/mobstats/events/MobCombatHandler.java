package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
            if (atk != null && atk.isInitialized()) {
                double mult = 1.0D
                        + atk.get(StatKeys.STR) * MobConfig.DAMAGE_PER_STR_POINT.get()
                        + atk.get(StatKeys.DEX) * MobConfig.DAMAGE_PER_DEX_POINT.get();
                mult *= Math.max(0.0D, atk.getDamageMultiplier());
                amount = (float) Math.max(0.0D, amount * mult);
                modified = true;
            }
        }

        MobStats def = MobStatsProvider.get(target).orElse(null);
        if (def != null && def.isInitialized()) {
            double reduction = def.get(StatKeys.VIT) * MobConfig.DAMAGE_REDUCTION_PER_VIT_POINT.get();
            double mult = Math.max(0.0D, 1.0D - reduction * def.getDefenseMultiplier());
            amount = (float) Math.max(0.0D, amount * mult);
            modified = true;
        }

        if (modified) {
            event.setAmount(amount);
            DamageProcessingGuard.markProcessedMob(target);
        }
    }
}
