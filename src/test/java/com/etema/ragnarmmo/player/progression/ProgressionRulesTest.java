package com.etema.ragnarmmo.player.progression;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressionRulesTest {
    @Test
    void defaultsForTestsDoesNotRequireLoadedConfig() {
        ProgressionRules rules = ProgressionRules.defaultsForTests(
                ResourceLocation.fromNamespaceAndPath("ragnarmmo", "novice"));

        assertEquals(10, rules.maxBaseLevel());
        assertEquals(10, rules.maxJobLevel());
        assertNotNull(rules.baseLevelCurve());
    }

    @Test
    void rejectsInvalidRules() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProgressionRules(0, 50, 48, 3, true, 1.0D, 1.0D, 0.0D, 0.0D,
                        new FormulaLevelCurve(50, 100.0, 1.15, 4.0, 0.05),
                        new FormulaLevelCurve(25, 50.0, 1.12, 3.5, 0.04)));
    }
}
