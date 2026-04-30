package com.etema.ragnarmmo.player.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RoKillCreditSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void killExperienceUsesRoKillCreditInsteadOfVanillaLastHurtByMob() throws IOException {
        String source = Files.readString(ROOT.resolve("player/stats/event/CommonEvents.java"));
        String deathHandler = methodBody(source, "public static void onDeath");

        assertTrue(deathHandler.contains("RoKillCreditService.resolveKiller"),
                "kill EXP must resolve killer through the RO combat-domain credit service");
        assertFalse(deathHandler.contains("getLastHurtByMob"),
                "kill EXP must not depend on vanilla lastHurtByMob credit");
        assertFalse(deathHandler.contains("getLastHurtByMobTimestamp"),
                "kill EXP must not depend on vanilla lastHurtByMob timing");
    }

    @Test
    void combatEngineRecordsRoKillCreditBeforeApplyingFinalDamage() throws IOException {
        String engine = Files.readString(ROOT.resolve("combat/engine/RagnarCombatEngine.java"));

        int record = engine.indexOf("RoKillCreditService.recordPlayerContribution");
        int hurt = engine.indexOf("SkillDamageHelper.dealSkillDamage", record);
        int clear = engine.indexOf("RoKillCreditService.clearPlayerContribution", hurt);

        assertTrue(record >= 0, "combat engine must record RO kill credit for applied combat resolutions");
        assertTrue(hurt > record, "kill credit must be present before hurt() can synchronously fire death handling");
        assertTrue(clear > hurt, "rejected final application must clear the provisional RO kill credit");
    }

    @Test
    void partySharingKeepsBaseAndJobExpSeparate() throws IOException {
        String party = Files.readString(ROOT.resolve("player/party/PartyXpService.java"));
        String commonEvents = Files.readString(ROOT.resolve("player/stats/event/CommonEvents.java"));

        assertTrue(party.contains("record PartyXpAward(int baseExp, int jobExp)"));
        assertTrue(party.contains("giveXpToMember(member, sharedBaseXp, sharedJobXp"));
        assertTrue(commonEvents.contains("PartyXpService.PartyXpAward"));
        assertFalse(commonEvents.contains("double jobRatio"),
                "party sharing must not reconstruct job EXP from base EXP ratio after distribution");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        int next = source.indexOf("\n    @SubscribeEvent", start + signature.length());
        if (next < 0) {
            next = source.length();
        }
        return source.substring(start, next);
    }
}
