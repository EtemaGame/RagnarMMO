package com.etema.ragnarmmo.combat.resolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatResolverArchitectureSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void combatMathDelegatesMinecraftBackedResolutionToResolverServices() throws IOException {
        String combatMath = Files.readString(ROOT.resolve("player/stats/compute/CombatMath.java"));

        assertTrue(combatMath.contains("WeaponClassificationService.isRangedWeapon"));
        assertTrue(combatMath.contains("WeaponClassificationService.baseAspd"));
        assertTrue(combatMath.contains("TargetCombatProfileResolver.tryGetTargetLevel"));
        assertTrue(combatMath.contains("MobCombatProfileResolver.tryGetResolvedMobHit"));
        assertFalse(combatMath.contains("MobProfileProvider.get(entity)"));
        assertFalse(combatMath.contains("MobConsumerReadViewResolver.resolve(entity)"));
    }

    @Test
    void formulaServicesStayFreeOfMinecraftAndForgeAfterResolverExtraction() throws IOException {
        try (var files = Files.list(ROOT.resolve("combat/formula"))) {
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
}
