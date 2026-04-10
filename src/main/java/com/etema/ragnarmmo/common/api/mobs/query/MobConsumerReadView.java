package com.etema.ragnarmmo.common.api.mobs.query;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Shared normalized read view for mob consumers during coexistence.
 *
 * <p>This is a read-only integration model only. It is not a runtime authority, does not expose
 * raw legacy tier values, and does not fold world-state policy into rank.</p>
 */
public record MobConsumerReadView(
        ResourceLocation entityTypeId,
        MobConsumerDataOrigin dataOrigin,
        int level,
        MobRank rank,
        String race,
        String element,
        String size,
        boolean isBossLikeForCompatibility,
        boolean hasNewRuntimeProfile,
        @Nullable MobConsumerInspectionStatsView inspectionStats) {

    public MobConsumerReadView {
        entityTypeId = Objects.requireNonNull(entityTypeId, "entityTypeId");
        dataOrigin = Objects.requireNonNull(dataOrigin, "dataOrigin");
        rank = Objects.requireNonNull(rank, "rank");
        race = requireNonBlank(race, "race");
        element = requireNonBlank(element, "element");
        size = requireNonBlank(size, "size");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
