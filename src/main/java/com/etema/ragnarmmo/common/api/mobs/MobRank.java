package com.etema.ragnarmmo.common.api.mobs;

/**
 * Semantic encounter category for the new mob system.
 *
 * <p>This type is intentionally separate from {@link MobTier}, which remains a compatibility model for
 * world-state persistence only.
 * {@code MobRank} does not carry lifecycle, persistence, or world-state semantics.</p>
 */
public enum MobRank {
    NORMAL,
    ELITE,
    MINI_BOSS,
    BOSS,
    MVP
}
