package com.etema.ragnarmmo.combat.formula;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatFormulaArchitectureSourceTest {
    private static final Path FORMULA_DIR = Path.of("src/main/java/com/etema/ragnarmmo/combat/formula");
    private static final Path COMBAT_MATH = Path.of("src/main/java/com/etema/ragnarmmo/player/stats/compute/CombatMath.java");

    @Test
    void pureFormulaServicesDoNotImportMinecraftOrForgeRuntimeTypes() throws IOException {
        try (var files = Files.list(FORMULA_DIR)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    assertFalse(source.contains("import net.minecraft."), () -> path + " imports Minecraft runtime types");
                    assertFalse(source.contains("import net.minecraftforge."), () -> path + " imports Forge runtime types");
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    void combatMathRemainsFacadeForExtractedPureFormulas() throws IOException {
        String source = Files.readString(COMBAT_MATH);

        assertTrue(source.contains("DamageFormulaService.statusAtk"));
        assertTrue(source.contains("AccuracyFormulaService.hit"));
        assertTrue(source.contains("DefenseFormulaService.softDef"));
        assertTrue(source.contains("AspdFormulaService.aspdRo"));
        assertTrue(source.contains("ResourceFormulaService.maxHp"));
        assertTrue(source.contains("StatusResistanceService.chanceByVitAndLuk"));
    }
}
