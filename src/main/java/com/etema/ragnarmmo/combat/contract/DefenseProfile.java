package com.etema.ragnarmmo.combat.contract;

public record DefenseProfile(
        double flee,
        double perfectDodge,
        double critShield,
        int vit,
        int agi,
        int intel,
        int luk,
        int level,
        double hardDef,
        double hardMdef) {
}
