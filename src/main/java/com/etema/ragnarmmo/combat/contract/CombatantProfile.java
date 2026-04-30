package com.etema.ragnarmmo.combat.contract;

import net.minecraft.world.entity.LivingEntity;

public record CombatantProfile(
        LivingEntity entity,
        CombatantKind kind,
        CombatStats stats,
        PhysicalAttackProfile physicalAttack,
        MagicAttackProfile magicAttack,
        DefenseProfile defense,
        CombatModifiers modifiers,
        boolean fallback) {
    public CombatantProfile(
            LivingEntity entity,
            CombatStats stats,
            PhysicalAttackProfile physicalAttack,
            MagicAttackProfile magicAttack,
            DefenseProfile defense,
            CombatModifiers modifiers,
            boolean fallback) {
        this(entity, CombatantKind.MOB, stats, physicalAttack, magicAttack, defense, modifiers, fallback);
    }
}
