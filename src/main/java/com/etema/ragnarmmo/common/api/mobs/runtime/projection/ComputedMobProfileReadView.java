package com.etema.ragnarmmo.common.api.mobs.runtime.projection;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;

import java.util.Objects;

/**
 * Read-only projection derived from {@link ComputedMobProfile}.
 *
 * <p>This view is downstream of the runtime authority and exists only to expose normalized read
 * data. It is not a second runtime authority and does not carry world-state policy.</p>
 */
public record ComputedMobProfileReadView(
        int level,
        MobRank rank,
        String race,
        String element,
        String size) {

    public static ComputedMobProfileReadView from(ComputedMobProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return new ComputedMobProfileReadView(
                profile.level(),
                profile.rank(),
                profile.race(),
                profile.element(),
                profile.size());
    }

    /**
     * Derived display hint for downstream read consumers.
     */
    public boolean isBossLikeForCompatibility() {
        return rank == MobRank.BOSS;
    }
}
