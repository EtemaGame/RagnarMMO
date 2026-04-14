package com.etema.ragnarmmo.mobs;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalManualMobEntryTest {

    @Test
    void rejectsInvalidConstraints() {
        assertThrows(IllegalArgumentException.class, () -> new InternalManualMobEntry(
                new net.minecraft.resources.ResourceLocation("minecraft:zombie"),
                true,
                0,
                MobRank.NORMAL,
                "race",
                "element",
                "size",
                50,
                5,
                8,
                0,
                0,
                0,
                0,
                0,
                140,
                0.23D,
                "",
                "test",
                1L));

        assertThrows(IllegalArgumentException.class, () -> new InternalManualMobEntry(
                new net.minecraft.resources.ResourceLocation("minecraft:zombie"),
                true,
                1,
                MobRank.NORMAL,
                "race",
                "element",
                "size",
                50,
                10,
                5,
                0,
                0,
                0,
                0,
                0,
                140,
                0.23D,
                "",
                "test",
                1L));
    }

    @Test
    void acceptsValidEntry() {
        assertDoesNotThrow(() -> new InternalManualMobEntry(
                new net.minecraft.resources.ResourceLocation("minecraft:zombie"),
                true,
                1,
                MobRank.NORMAL,
                "race",
                "element",
                "size",
                50,
                5,
                10,
                0,
                0,
                0,
                0,
                0,
                140,
                0.23D,
                "",
                "test",
                1L));
    }
}
