package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import net.minecraft.resources.ResourceLocation;

public final class InternalManualMobEntryValidator {

    private static final int MIN_ASPD = 1;

    private InternalManualMobEntryValidator() {
    }

    public static void validateOrThrow(InternalManualMobEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry is null");
        }
        validateOrThrow(
                entry.entityTypeId(),
                entry.level(),
                entry.rank(),
                entry.atkMin(),
                entry.atkMax(),
                entry.def(),
                entry.mdef(),
                entry.hit(),
                entry.flee(),
                entry.crit(),
                entry.aspd(),
                entry.moveSpeed());
    }

    public static void validateOrThrow(
            ResourceLocation entityTypeId,
            int level,
            MobRank rank,
            int atkMin,
            int atkMax,
            int def,
            int mdef,
            int hit,
            int flee,
            int crit,
            int aspd,
            double moveSpeed) {
        if (entityTypeId == null) {
            throw new IllegalArgumentException("entityTypeId must not be null");
        }
        if (rank == null) {
            throw new IllegalArgumentException("rank must not be null");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        if (atkMin < 0) {
            throw new IllegalArgumentException("atkMin must be >= 0");
        }
        if (atkMax < atkMin) {
            throw new IllegalArgumentException("atkMax must be >= atkMin");
        }
        if (def < 0 || mdef < 0 || hit < 0 || flee < 0 || crit < 0) {
            throw new IllegalArgumentException("def/mdef/hit/flee/crit must be >= 0");
        }
        if (aspd < MIN_ASPD) {
            throw new IllegalArgumentException("aspd must be >= " + MIN_ASPD);
        }
        if (moveSpeed <= 0D) {
            throw new IllegalArgumentException("moveSpeed must be > 0");
        }
    }
}
