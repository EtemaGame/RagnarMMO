package com.etema.ragnarmmo.mobs.util;

import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.mobs.world.MobSpawnOverrides;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class MobUtils {

    /**
     * Identifies whether a mob should be treated as boss-like by gameplay systems.
     */
    public static boolean isBossLike(LivingEntity mob) {
        var readView = MobConsumerReadViewResolver.resolve(mob);

        if (readView.isPresent()) {
            return readView.get().bossLike();
        }

        if (MobSpawnOverrides.isForcedBoss(mob)) {
            return true;
        }

        return mob instanceof WitherBoss || mob instanceof EnderDragon;
    }

}
