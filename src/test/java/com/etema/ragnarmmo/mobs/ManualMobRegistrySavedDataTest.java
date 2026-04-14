package com.etema.ragnarmmo.mobs;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistrySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualMobRegistrySavedDataTest {

    @Test
    void skipsCorruptEntriesOnLoad() {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", 1);
        ListTag list = new ListTag();

        InternalManualMobEntry valid = new InternalManualMobEntry(
                new net.minecraft.resources.ResourceLocation("minecraft:zombie"),
                true,
                1,
                MobRank.NORMAL,
                "race",
                "element",
                "size",
                50,
                5,
                8,
                0,
                0,
                10,
                5,
                1,
                140,
                0.23D,
                "",
                "test",
                1L);
        list.add(valid.save());

        CompoundTag corrupt = new CompoundTag();
        corrupt.putString("entityTypeId", "minecraft:skeleton");
        corrupt.putInt("level", 0);
        corrupt.putString("rank", "NORMAL");
        list.add(corrupt);

        root.put("entries", list);

        ManualMobRegistrySavedData data = ManualMobRegistrySavedData.load(root);
        assertEquals(1, data.values().size());
    }
}
