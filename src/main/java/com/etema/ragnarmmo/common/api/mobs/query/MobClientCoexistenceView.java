package com.etema.ragnarmmo.common.api.mobs.query;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

import java.util.Objects;

/**
 * Minimal client-side coexistence projection for mobs backed by the new runtime source.
 *
 * <p>This is a read-only synced view only. It is not a second runtime authority and does not
 * expose raw legacy tier semantics.</p>
 */
public record MobClientCoexistenceView(
        int level,
        MobRank rank,
        String race,
        String element,
        String size,
        MobConsumerDataOrigin dataOrigin) {

    public MobClientCoexistenceView {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        rank = Objects.requireNonNull(rank, "rank");
        race = requireNonBlank(race, "race");
        element = requireNonBlank(element, "element");
        size = requireNonBlank(size, "size");
        dataOrigin = Objects.requireNonNull(dataOrigin, "dataOrigin");
    }

    public static MobClientCoexistenceView fromReadView(MobConsumerReadView readView) {
        Objects.requireNonNull(readView, "readView");
        return new MobClientCoexistenceView(
                readView.level(),
                readView.rank(),
                readView.race(),
                readView.element(),
                readView.size(),
                readView.dataOrigin());
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
