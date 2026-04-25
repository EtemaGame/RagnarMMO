package com.etema.ragnarmmo.common.api.mobs.data;

import org.jetbrains.annotations.Nullable;

/**
 * Declarative {@code ro_stats} block for authored mob data.
 *
 * <p>This block is authored/resolved declarative data only. It is not a runtime-final combat profile.</p>
 */
public record MobRoStatsBlock(
        @Nullable Integer str,
        @Nullable Integer agi,
        @Nullable Integer vit,
        @Nullable Integer int_,
        @Nullable Integer dex,
        @Nullable Integer luk) {
}
