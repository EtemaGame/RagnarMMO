package com.etema.ragnarmmo.common.api.mobs.runtime.projection;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;

import java.util.Objects;

/**
 * Read-only coexistence projection derived from {@link ComputedMobProfile}.
 *
 * <p>This view is downstream of the new runtime-final authority and exists only to expose normalized
 * read data for later compatibility bridges. It is not a second runtime authority, does not expose raw
 * legacy tier semantics, and does not carry world-state policy.</p>
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
     * Temporary coexistence hint for downstream compatibility layers.
     *
     * <p>This is derived only from semantic rank and does not imply boss persistence, lifecycle, or
     * any other world-state policy.</p>
     */
    public boolean isBossLikeForCompatibility() {
        return rank == MobRank.BOSS;
    }
}
