package com.etema.ragnarmmo.mobs.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MobRoBaseStatsSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void mobCombatResolverUsesRoBaseStatsInsteadOfReconstructingFromDerivedFields() throws IOException {
        String resolver = read("combat/contract/CombatantProfileResolver.java");

        assertTrue(resolver.contains("profile.baseStats()"),
                "Mob combat profiles must use authored/procedural RO base stats");
        assertFalse(resolver.contains("dexEstimate"),
                "Mob combat profiles must not reconstruct DEX from HIT");
        assertFalse(resolver.contains("agiEstimate"),
                "Mob combat profiles must not reconstruct AGI from FLEE");
        assertFalse(resolver.contains("vitEstimate"),
                "Mob combat profiles must not reconstruct VIT from DEF");
        assertFalse(resolver.contains("intEstimate"),
                "Mob combat profiles must not reconstruct INT from MDEF");
    }

    @Test
    void mobProfilePersistsAndSyncsRoBaseStats() throws IOException {
        String state = read("mobs/capability/MobProfileState.java");
        String packet = read("mobs/network/SyncMobProfilePacket.java");

        assertTrue(state.contains("\"BaseStr\""));
        assertTrue(state.contains("\"BaseDex\""));
        assertTrue(packet.contains("profile.baseStats().str()"));
        assertTrue(packet.contains("new RoBaseStats("));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
