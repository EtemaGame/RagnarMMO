package com.etema.ragnarmmo.common.api.mobs.data;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.profile.MobTier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Declarative authored mob definition after template expansion and override resolution.
 *
 * <p>This type is still declarative data. It is not the runtime-final mob profile.</p>
 */
public record ResolvedMobDefinition(
        ResourceLocation entity,
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
