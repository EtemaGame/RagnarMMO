package com.etema.ragnarmmo.mobs.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record AuthoredMobDefinition(
        ResourceLocation entityType,
        Optional<String> race,
        Optional<String> element,
        Optional<String> size,
        OptionalInt baseHp,
        OptionalInt atkMin,
        OptionalInt atkMax,
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
        race = race == null ? Optional.empty() : race;
        element = element == null ? Optional.empty() : element;
        size = size == null ? Optional.empty() : size;
        baseHp = baseHp == null ? OptionalInt.empty() : baseHp;
        atkMin = atkMin == null ? OptionalInt.empty() : atkMin;
        atkMax = atkMax == null ? OptionalInt.empty() : atkMax;
        def = def == null ? OptionalInt.empty() : def;
        mdef = mdef == null ? OptionalInt.empty() : mdef;
        hit = hit == null ? OptionalInt.empty() : hit;
        flee = flee == null ? OptionalInt.empty() : flee;
        crit = crit == null ? OptionalInt.empty() : crit;
        aspd = aspd == null ? OptionalInt.empty() : aspd;
        moveSpeed = moveSpeed == null ? OptionalDouble.empty() : moveSpeed;
    }
}
