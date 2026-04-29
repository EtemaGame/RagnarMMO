package com.etema.ragnarmmo.combat.contract;

import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;

public record CombatModifiers(
        String race,
        ElementType element,
        CombatMath.MobSize size) {
    public CombatModifiers {
        race = race == null || race.isBlank() ? "unknown" : race;
        element = element == null ? ElementType.NEUTRAL : element;
        size = size == null ? CombatMath.MobSize.MEDIUM : size;
    }
}
