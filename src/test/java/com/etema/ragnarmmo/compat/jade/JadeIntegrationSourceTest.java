package com.etema.ragnarmmo.compat.jade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JadeIntegrationSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void mobProfileDisplayIsDelegatedToJade() throws IOException {
        String plugin = Files.readString(ROOT.resolve("compat/jade/RagnarJadePlugin.java"));
        String provider = Files.readString(ROOT.resolve("compat/jade/RagnarMobJadeProvider.java"));
        Path oldNameplateRenderer = ROOT.resolve("client/render/RagnarBarRenderHandler.java");
        Path oldMobHurtSync = ROOT.resolve("mobs/event/ServerMobEvents.java");
        Path oldTargetFrame = ROOT.resolve("client/ui/TargetFrameOverlay.java");

        assertTrue(plugin.contains("@WailaPlugin"));
        assertTrue(plugin.contains("registerEntityDataProvider"));
        assertTrue(plugin.contains("registerEntityComponent"));
        assertTrue(provider.contains("MobProfileProvider.get"));
        assertTrue(provider.contains("appendServerData"));
        assertTrue(provider.contains("appendTooltip"));
        assertTrue(provider.contains("jade.ragnarmmo.level"));
        assertFalse(Files.exists(oldNameplateRenderer), "custom overhead mob bars should stay removed");
        assertFalse(Files.exists(oldMobHurtSync), "cosmetic per-hurt mob sync should stay removed");
        assertFalse(Files.exists(oldTargetFrame), "custom target frame should stay removed while Jade owns mob display");
    }
}
