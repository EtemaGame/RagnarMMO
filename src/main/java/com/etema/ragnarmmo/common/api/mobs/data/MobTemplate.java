package com.etema.ragnarmmo.common.api.mobs.data;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.profile.MobTier;
import org.jetbrains.annotations.Nullable;

/**
 * Declarative reusable payload for an authored mob template.
 *
 * <p>The template resource id belongs to the datapack resource layer and is intentionally not embedded in
 * this payload model.</p>
 */
public record MobTemplate(
        @Nullable MobRank rank,
        @Nullable MobTier tier,
        @Nullable Integer level,
        @Nullable Integer baseExp,
        @Nullable Integer jobExp,
        @Nullable MobRoStatsBlock roStats,
        @Nullable MobDirectStatsBlock directStats,
        @Nullable String race,
        @Nullable String element,
        @Nullable String size) {
}
