package com.etema.ragnarmmo.common.api.mobs.data;

import org.jetbrains.annotations.Nullable;

/**
 * Declarative {@code direct_stats} block for authored mob data.
 *
 * <p>This block carries authored or resolved declarative combat values only. It is not a runtime-final
 * profile and does not imply application to Minecraft attributes.</p>
 */
public record MobDirectStatsBlock(
        @Nullable Integer maxHp,
        @Nullable Integer atkMin,
        @Nullable Integer atkMax,
        @Nullable Integer def,
        @Nullable Integer mdef,
        @Nullable Integer hit,
        @Nullable Integer flee,
        @Nullable Integer crit,
        @Nullable Integer aspd,
        @Nullable Double moveSpeed) {
}
