package com.etema.ragnarmmo.common.api.mobs.data;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Declarative manual mob definition authored for one exact {@code entity_type} target.
 *
 * <p>This type represents external authored data only. It is not runtime-final and does not contain
 * application or world-state semantics.</p>
 */
public record MobDefinition(
        ResourceLocation entity,
        @Nullable ResourceLocation template,
        @Nullable MobRank rank,
        @Nullable Integer level,
        @Nullable MobRoStatsBlock roStats,
        @Nullable MobDirectStatsBlock directStats,
        @Nullable String race,
        @Nullable String element,
        @Nullable String size) {
}
