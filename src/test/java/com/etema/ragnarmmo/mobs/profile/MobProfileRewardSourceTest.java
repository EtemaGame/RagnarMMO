package com.etema.ragnarmmo.mobs.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobProfileRewardSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void killExperienceComesFromMobProfileInsteadOfVanillaHpOrArmor() throws IOException {
        String source = Files.readString(ROOT.resolve("player/stats/event/CommonEvents.java"));
        String method = methodBody(source, "private static KillExp computeKillExp");

        assertTrue(method.contains("profile.baseExp()"));
        assertTrue(method.contains("profile.jobExp()"));
        assertFalse(method.contains("getMaxHealth()"), "kill EXP must not derive from vanilla max health");
        assertFalse(method.contains("getArmorValue()"), "kill EXP must not derive from vanilla armor");
    }

    @Test
    void mobCombatHandlerDoesNotMutateVanillaDamageAmounts() throws IOException {
        String source = Files.readString(ROOT.resolve("mobs/event/MobCombatHandler.java"));

        assertFalse(source.contains("event.getAmount()"));
        assertFalse(source.contains("event.setAmount("));
    }

    @Test
    void difficultyResolverIncludesDistanceAndZoneLevelSources() throws IOException {
        String source = Files.readString(ROOT.resolve("mobs/difficulty/MobDifficultyResolver.java"));

        assertTrue(source.contains("case REGION"));
        assertTrue(source.contains("case DISTANCE"));
        assertTrue(source.contains("context.structureId()"));
        assertTrue(source.contains("context.dimension()"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        int next = source.indexOf("\n    /**", start + signature.length());
        if (next < 0) {
            next = source.indexOf("\n    private static", start + signature.length());
        }
        return source.substring(start, next);
    }
}
