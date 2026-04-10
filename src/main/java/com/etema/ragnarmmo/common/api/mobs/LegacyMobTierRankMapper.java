package com.etema.ragnarmmo.common.api.mobs;

import java.util.Objects;

/**
 * Temporary compatibility boundary from legacy {@link MobTier} to semantic {@link MobRank}.
 *
 * <p>This mapper only translates encounter category and intentionally does not carry legacy boss,
 * persistence, multiplier, or world-state semantics.</p>
 */
public final class LegacyMobTierRankMapper {

    private LegacyMobTierRankMapper() {
    }

    public static MobRank toMobRank(MobTier tier) {
        return switch (Objects.requireNonNull(tier, "tier")) {
            case NORMAL -> MobRank.NORMAL;
            case ELITE, MINI_BOSS -> MobRank.ELITE;
            case BOSS, MVP -> MobRank.BOSS;
        };
    }

    /**
     * Conservative compatibility projection from semantic {@link MobRank} back into legacy
     * {@link MobTier}.
     *
     * <p>This exists only for old paths that still persist or branch on legacy tier values.
     * The mapping intentionally chooses the lower compatibility representative when the semantic
     * rank has already collapsed old distinctions such as MINI_BOSS/MVP.</p>
     */
    public static MobTier toCompatibilityTier(MobRank rank) {
        return switch (Objects.requireNonNull(rank, "rank")) {
            case NORMAL -> MobTier.NORMAL;
            case ELITE -> MobTier.ELITE;
            case BOSS -> MobTier.BOSS;
        };
    }
}
