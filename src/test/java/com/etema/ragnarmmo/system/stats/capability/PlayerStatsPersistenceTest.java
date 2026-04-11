package com.etema.ragnarmmo.system.stats.capability;

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
        CompoundTag statTag = tag.getCompound("Stats");

        assertEquals(1, tag.getInt("Level"));
        assertTrue(statTag.contains("str"));
        assertFalse(statTag.contains("level"));
    }
}
