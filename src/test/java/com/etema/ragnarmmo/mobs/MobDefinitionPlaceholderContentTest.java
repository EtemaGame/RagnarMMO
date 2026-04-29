package com.etema.ragnarmmo.mobs;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobDefinitionPlaceholderContentTest {
    @Test
    void p3PlaceholderMobDefinitionsAreNotShippedAsRealContent() {
        assertFalse(Files.exists(Path.of("src/main/resources/data/ragnarmmo/mob_definitions/vanilla/zombie.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/ragnarmmo/mob_definitions/vanilla/skeleton.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/ragnarmmo/mob_definitions/vanilla/blaze.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/ragnarmmo/mob_templates/early_undead.json")));
    }
}
