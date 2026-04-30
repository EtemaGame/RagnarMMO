package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CombatContractSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void roCombatEngineDoesNotReadVanillaDamageAmountAsBalanceInput() throws IOException {
        String engine = read("combat/engine/RagnarCombatEngine.java");

        assertFalse(engine.contains("e.getAmount()"),
                "RO combat resolution must not use LivingHurtEvent#getAmount() as damage input");
    }

    @Test
    void roCombatResolutionDoesNotReadVanillaEntityAttributesAsBalanceInput() throws IOException {
        String sources = read("combat/engine/RagnarCombatEngine.java")
                + read("combat/contract/CombatantProfileResolver.java")
                + read("combat/contract/CombatContract.java")
                + read("player/stats/compute/EquipmentStatSnapshot.java");

        assertFalse(sources.contains("Attributes.ATTACK_DAMAGE"),
                "RO combat resolution must not read vanilla attack damage");
        assertFalse(sources.contains("Attributes.ARMOR"),
                "RO combat resolution must not read vanilla armor");
        assertFalse(sources.contains("Attributes.MAX_HEALTH"),
                "RO combat resolution must not read vanilla max health");
        assertFalse(sources.contains("getArmorValue()"),
                "RO combat resolution must not read vanilla armor value");
        assertFalse(sources.contains("getMaxHealth()"),
                "RO combat resolution must not derive combat stats from vanilla max health");
    }

    @Test
    void combatContractDefinesServerSideDamageMinimum() throws IOException {
        String contract = read("combat/contract/CombatContract.java");

        assertTrue(contract.contains("Math.max(1.0D, damage)"),
                "Combat contract must guarantee minimum damage unless immunity is added explicitly");
    }

    @Test
    void physicalCritBypassesPhysicalDefenseButKeepsModifiers() throws IOException {
        String contract = read("combat/contract/CombatContract.java");
        int modifiers = contract.indexOf("damage = damageCalculator.applyModifiers(");
        int defense = contract.indexOf("if (!critical) {\n            damage = damageCalculator.applyPhysicalDefense(");

        assertTrue(modifiers >= 0, "Physical damage must still apply size/element/race modifiers");
        assertTrue(defense > modifiers, "Physical DEF must be skipped only after modifiers when critical");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
