package com.etema.ragnarmmo.common.api.mobs.combat;

import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority;
import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthorityResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Normalized view of mob stats for combat calculations.
 */
public record MobCombatRuntimeView(
    int level,
    double damageMultiplier,
    double defenseMultiplier
) {
    public static MobCombatRuntimeView fromLegacy(MobStats stats) {
        return new MobCombatRuntimeView(
            stats.getLevel(),
            stats.getDamageMultiplier(),
            stats.getDefenseMultiplier()
        );
    }

    public static MobCombatRuntimeView strict(ComputedMobProfile profile) {
        return new MobCombatRuntimeView(profile.level(), 1.0, 1.0);
    }
}
