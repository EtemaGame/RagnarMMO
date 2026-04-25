package com.etema.ragnarmmo.player.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ProgressionServiceTest {

    @Test
    void baseExpLevelsUpAndAwardsPoints() {
        ResourceLocation novice = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "novice");
        PlayerProgressionService service = new PlayerProgressionService(ProgressionRules.defaultsForTests(novice));
        PlayerProgression progression = new PlayerProgression(1, 0, 1, 0, 0, 0,
                novice);

        ProgressionResult result = service.addBaseExp(progression, 10_000);

        assertTrue(result.baseLevelsGained() > 0);
        assertTrue(result.progression().statPoints() > 0);
    }
}
