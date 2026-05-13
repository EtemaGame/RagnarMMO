package com.etema.ragnarmmo.skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SkillXpArchitectureSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void skillEventsDelegatesCombatXpAndWeaponResolution() throws IOException {
        String events = Files.readString(ROOT.resolve("skills/runtime/SkillEvents.java"));
        String award = Files.readString(ROOT.resolve("skills/xp/SkillXpAwardService.java"));
        String weapon = Files.readString(ROOT.resolve("skills/resolver/WeaponSkillResolver.java"));
        String source = Files.readString(ROOT.resolve("skills/xp/SkillXpSourceResolver.java"));

        assertTrue(events.contains("SkillXpAwardService.awardCombatXp"));
        assertFalse(events.contains("xp = 10"));
        assertFalse(events.contains("DAGGER_TAG"));
        assertTrue(award.contains("XPGainReason.COMBAT_PVE"));
        assertTrue(weapon.contains("applicableCombatSkills"));
        assertTrue(source.contains("SkillXpConfig.combatFallbackXp()"));
    }

    @Test
    void skillJsonWithoutEffectMustDeclareRuntimeReason() throws IOException {
        Path skillDir = Path.of("src/main/resources/data/ragnarmmo/skills");
        try (var files = Files.list(skillDir)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    String json = Files.readString(path);
                    if (!json.contains("\"effect_class\"")) {
                        assertTrue(json.contains("\"runtime\""), () -> path + " is missing runtime metadata");
                        assertTrue(json.contains("\"effect_required\""), () -> path + " is missing effect_required");
                        assertTrue(json.contains("\"reason\""), () -> path + " is missing runtime.reason");
                    }
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
