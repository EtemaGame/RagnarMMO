package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record AuthoredMobDefinition(
        ResourceLocation entityType,
        Optional<MobRank> baselineRank,
        Optional<MobTier> tier,
        OptionalInt baseLevel,
        Optional<RoBaseStats> baseStats,
        Optional<String> race,
        Optional<String> element,
        Optional<String> size,
        OptionalInt baseExp,
        OptionalInt jobExp,
        OptionalInt baseHp,
        OptionalInt atkMin,
        OptionalInt atkMax,
        OptionalInt matkMin,
        OptionalInt matkMax,
        OptionalInt def,
        OptionalInt mdef,
        OptionalInt hit,
        OptionalInt flee,
        OptionalInt crit,
        OptionalInt aspd,
        OptionalDouble moveSpeed) {
    public AuthoredMobDefinition {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType must not be null");
        }
        baselineRank = baselineRank == null ? Optional.empty() : baselineRank;
        tier = tier == null ? Optional.empty() : tier;
        baseLevel = baseLevel == null ? OptionalInt.empty() : baseLevel;
        baseStats = baseStats == null ? Optional.empty() : baseStats;
        race = race == null ? Optional.empty() : race;
        element = element == null ? Optional.empty() : element;
        size = size == null ? Optional.empty() : size;
        baseExp = baseExp == null ? OptionalInt.empty() : baseExp;
        jobExp = jobExp == null ? OptionalInt.empty() : jobExp;
        baseHp = baseHp == null ? OptionalInt.empty() : baseHp;
        atkMin = atkMin == null ? OptionalInt.empty() : atkMin;
        atkMax = atkMax == null ? OptionalInt.empty() : atkMax;
        matkMin = matkMin == null ? OptionalInt.empty() : matkMin;
        matkMax = matkMax == null ? OptionalInt.empty() : matkMax;
        def = def == null ? OptionalInt.empty() : def;
        mdef = mdef == null ? OptionalInt.empty() : mdef;
        hit = hit == null ? OptionalInt.empty() : hit;
        flee = flee == null ? OptionalInt.empty() : flee;
        crit = crit == null ? OptionalInt.empty() : crit;
        aspd = aspd == null ? OptionalInt.empty() : aspd;
        moveSpeed = moveSpeed == null ? OptionalDouble.empty() : moveSpeed;
    }

    public AuthoredMobDefinition(
            ResourceLocation entityType,
            Optional<MobRank> baselineRank,
            Optional<MobTier> tier,
            OptionalInt baseLevel,
            Optional<String> race,
            Optional<String> element,
            Optional<String> size,
            OptionalInt baseExp,
            OptionalInt jobExp,
            OptionalInt baseHp,
            OptionalInt atkMin,
            OptionalInt atkMax,
            OptionalInt matkMin,
            OptionalInt matkMax,
            OptionalInt def,
            OptionalInt mdef,
            OptionalInt hit,
            OptionalInt flee,
            OptionalInt crit,
            OptionalInt aspd,
            OptionalDouble moveSpeed) {
        this(entityType, baselineRank, tier, baseLevel, Optional.empty(), race, element, size, baseExp, jobExp,
                baseHp, atkMin, atkMax, matkMin, matkMax, def, mdef, hit, flee, crit, aspd, moveSpeed);
    }
}
