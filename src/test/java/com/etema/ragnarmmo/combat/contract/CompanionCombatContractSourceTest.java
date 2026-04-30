package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CompanionCombatContractSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void combatantProfilesClassifyPlayersMobsAndCompanionsExplicitly() throws IOException {
        String profile = read("combat/contract/CombatantProfile.java");
        String resolver = read("combat/contract/CombatantProfileResolver.java");

        assertTrue(profile.contains("CombatantKind kind"));
        assertTrue(resolver.contains("CombatantKind.PLAYER"));
        assertTrue(resolver.contains("CombatantKind.COMPANION"));
        assertTrue(resolver.contains("MobProfileEligibility.isCompanion"));
        assertTrue(resolver.contains("CombatantKind.MOB"));
    }

    @Test
    void companionDamageUsesCombatContractAndOwnerKillCredit() throws IOException {
        String commonEvents = read("player/stats/event/CommonEvents.java");
        String guard = read("common/util/DamageProcessingGuard.java");

        assertTrue(commonEvents.contains("processCompanionContractDamage"));
        assertTrue(commonEvents.contains("MobProfileEligibility.isCompanion"));
        assertTrue(commonEvents.contains("RagnarCombatEngine.get().contract().resolveBasicAttack"));
        assertTrue(commonEvents.contains("CompanionProfileService.resolveOnlineOwner"));
        assertTrue(commonEvents.contains("RoKillCreditService.recordPlayerContribution(owner"));
        assertTrue(guard.contains("markCompanionContractDamage"));
        assertFalse(commonEvents.contains("getLastHurtByMob"),
                "companion kill credit must not fall back to vanilla lastHurtByMob");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
