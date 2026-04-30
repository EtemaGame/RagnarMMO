package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecondaryCombatActionSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void autoCounterAndAutoBlitzAreEngineSecondaryActions() throws IOException {
        String engine = read("combat/engine/RagnarCombatEngine.java");
        String commonEvents = read("player/stats/event/CommonEvents.java");

        assertTrue(engine.contains("\"auto_counter\""));
        assertTrue(engine.contains("\"blitz_beat\""));
        assertTrue(engine.contains("triggerAutoCounter"));
        assertTrue(engine.contains("triggerAutoBlitz"));
        assertTrue(engine.contains("applySecondarySkillResolution"));
        assertTrue(engine.contains("DamageProcessingGuard.markSkillContractDamage"));
        assertTrue(commonEvents.contains("triggerAutoBlitzFromRangedAttack"));
    }

    @Test
    void legacySkillHurtDispatcherIsRemoved() throws IOException {
        String skillEffect = read("skills/api/ISkillEffect.java");
        String handler = read("skills/runtime/SkillEffectHandler.java");
        String autoCounter = read("skills/job/knight/AutoCounterSkillEffect.java");
        String blitzBeat = read("skills/job/hunter/BlitzBeatSkillEffect.java");

        assertTrue(!skillEffect.contains("OFFENSIVE_HURT"));
        assertTrue(!skillEffect.contains("DEFENSIVE_HURT"));
        assertTrue(!handler.contains("onOffensiveHurt"));
        assertTrue(!handler.contains("onDefensiveHurt"));
        assertTrue(!autoCounter.contains("LivingHurtEvent"));
        assertTrue(!blitzBeat.contains("LivingHurtEvent"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
