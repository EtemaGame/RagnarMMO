package com.etema.ragnarmmo.combat.balance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatBalanceSourceTest {
    private static final Path BALANCE_ROOT = Path.of("src/main/java/com/etema/ragnarmmo/combat/balance");

    @Test
    void balanceContractUsesFormulaFixturesInsteadOfPerMobCases() throws IOException {
        String sources = Files.readString(BALANCE_ROOT.resolve("BalanceMobFixture.java"))
                + Files.readString(BALANCE_ROOT.resolve("BalancePlayerFixture.java"))
                + Files.readString(BALANCE_ROOT.resolve("BalanceSimulator.java"));

        assertTrue(sources.contains("MobTier"));
        assertFalse(sources.contains("minecraft:"));
        assertFalse(sources.contains("zombie"));
        assertFalse(sources.contains("skeleton"));
        assertFalse(sources.contains("blaze"));
    }
}
