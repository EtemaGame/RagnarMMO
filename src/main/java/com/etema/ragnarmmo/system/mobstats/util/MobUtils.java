package com.etema.ragnarmmo.system.mobstats.util;

import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class MobUtils {

    /**
     * Identifies whether a mob should be treated as boss-like for compatibility-sensitive gameplay.
     *
     * <p>This helper is the primary leverage point for gameplay consumers (skills, effects)
     * during mob architecture coexistence.</p>
     */
    public static boolean isBossLikeForCompatibility(LivingEntity mob) {
        // Prioritize the shared read-surface mapping first so commands and gameplay
        // helpers agree on the same coexistence boundary.
        var readView = MobConsumerReadViewResolver.resolve(mob);

        if (readView.isPresent()) {
            return readView.get().isBossLikeForCompatibility();
        }

        if (MobSpawnOverrides.isManualBoss(mob)) {
            return true;
        }

        // Fallback: check vanilla boss types
        return mob instanceof WitherBoss || mob instanceof EnderDragon;
    }

    /**
     * Legacy gameplay alias kept for older call sites during coexistence.
     */
    public static boolean isMVPBoss(LivingEntity mob) {
        return isBossLikeForCompatibility(mob);
    }
}
