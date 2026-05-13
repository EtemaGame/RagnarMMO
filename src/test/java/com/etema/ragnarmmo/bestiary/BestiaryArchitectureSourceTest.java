package com.etema.ragnarmmo.bestiary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BestiaryArchitectureSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void categoryAndFilterTabKeepAllSeparated() throws IOException {
        String category = read("bestiary/api/BestiaryCategory.java");
        String filter = read("bestiary/api/BestiaryFilterTab.java");

        assertFalse(category.contains("ALL"), "BestiaryCategory must not contain ALL; ALL is a UI filter only");
        assertTrue(filter.contains("ALL"), "BestiaryFilterTab must expose ALL for UI filtering");
    }

    @Test
    void indexBuilderDoesNotUseCombatMobEligibility() throws IOException {
        String builder = read("bestiary/data/BestiaryIndexBuilder.java");

        assertFalse(builder.contains("MobProfileEligibility"),
                "Bestiary indexing must not reuse combat-only mob profile eligibility");
        assertTrue(builder.contains("ForgeRegistries.ENTITY_TYPES"),
                "Bestiary index must be built from the server-side entity registry");
    }

    @Test
    void requestIndexPacketHasNoClientControlledPayload() throws IOException {
        String packet = read("bestiary/network/RequestBestiaryIndexPacket.java");

        assertTrue(packet.contains("public record RequestBestiaryIndexPacket()"),
                "Bestiary index requests must not include filters or client-controlled data");
        assertTrue(packet.contains("BestiaryRegistry.getInstance().currentIndex()"),
                "Server must answer from the authoritative bestiary registry");
    }

    @Test
    void detailsUseSafeServerSideSources() throws IOException {
        String resolver = read("bestiary/data/BestiaryDetailsResolver.java");

        assertTrue(resolver.contains("CardRegistry.getInstance().getForMob"),
                "Bestiary card display must use deterministic getForMob, not rollDrop");
        assertFalse(resolver.contains("rollDrop("),
                "Bestiary UI must not roll card drops");
        assertTrue(resolver.contains("AuthoredMobProfileResolver.resolvePartialDefinition"),
                "RO stat previews must resolve server-side from authored mob definitions");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
