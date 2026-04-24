package com.etema.ragnarmmo.common.api.mobs.combat;

import com.etema.ragnarmmo.mobs.profile.MobProfile;

/**
 * Normalized view of mob stats for combat calculations.
 */
public record MobCombatRuntimeView(
    int level,
    double damageMultiplier,
    double defenseMultiplier
) {
    public static MobCombatRuntimeView fromProfile(MobProfile profile) {
        return new MobCombatRuntimeView(profile.level(), 1.0, 1.0);
    }
}
