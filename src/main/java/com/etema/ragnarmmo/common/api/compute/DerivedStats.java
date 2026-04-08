package com.etema.ragnarmmo.common.api.compute;

public class DerivedStats {
    public double physicalAttack;
    public double physicalAttackMin;
    public double physicalAttackMax;
    public double magicAttack;
    public double magicAttackMin;
    public double magicAttackMax;

    public double accuracy;
    public double criticalChance;
    public double criticalDamageMultiplier;
    public double flee;
    public double perfectDodge;

    // Display ASPD in RO terms (0-190). Runtime pacing still uses globalCooldown.
    public double attackSpeed;
    public double castTime;
    public double globalCooldown;

    public double physicalDamageReduction;
    public double magicDamageReduction;

    public double maxHealth;
    public double healthRegenPerSecond;

    // Mana: INT-based resource for magical classes (Mage, Priest, Wizard...)
    public double maxMana;
    public double manaRegenPerSecond;

    // SP: VIT/STR-based resource for physical classes (Swordman, Thief, Archer...)
    // Separate from Mana so each class has an appropriate stamina curve.
    public double maxSP;
    public double spRegenPerSecond;

    public double defense;
    public double magicDefense;
    public double hardDefense;
    public double softDefense;
    public double hardMagicDefense;
    public double softMagicDefense;

    public double lifeSteal;
    public double armorPierce;
    public double armorShred;
    public double overheal;
}
