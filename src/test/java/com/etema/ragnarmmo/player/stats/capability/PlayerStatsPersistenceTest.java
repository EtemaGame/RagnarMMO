package com.etema.ragnarmmo.player.stats.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PlayerStatsPersistenceTest {

    @Test
    void serializedStatsDoNotIncludeLevelPseudoStat() {
        PlayerStats stats = new PlayerStats();

        CompoundTag tag = stats.serializeNBT();
        CompoundTag progressionTag = tag.getCompound("Progression");
        CompoundTag statTag = tag.getCompound("PrimaryStats");

        assertEquals(1, progressionTag.getInt("BaseLevel"));
        assertTrue(statTag.contains("str"));
        assertFalse(statTag.contains("level"));
    }
}
