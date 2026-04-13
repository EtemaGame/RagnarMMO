package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ManualMobRegistrySavedData extends SavedData {

    private static final String DATA_NAME = "ragnarmmo_manual_mob_registry";
    private final Map<String, InternalManualMobEntry> entries = new LinkedHashMap<>();

    public static ManualMobRegistrySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                ManualMobRegistrySavedData::load,
                ManualMobRegistrySavedData::new,
                DATA_NAME);
    }

    public Optional<InternalManualMobEntry> get(String entityTypeId) {
        return Optional.ofNullable(entries.get(entityTypeId));
    }

    public Collection<InternalManualMobEntry> values() {
        return entries.values();
    }

    public void upsert(InternalManualMobEntry entry) {
        entries.put(entry.entityTypeId().toString(), entry);
        setDirty();
    }

    public boolean remove(String entityTypeId) {
        if (entries.remove(entityTypeId) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (InternalManualMobEntry entry : entries.values()) {
            list.add(entry.save());
        }
        tag.put("entries", list);
        return tag;
    }

    public static ManualMobRegistrySavedData load(CompoundTag tag) {
        ManualMobRegistrySavedData data = new ManualMobRegistrySavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                InternalManualMobEntry entry = InternalManualMobEntry.load(list.getCompound(i));
                data.entries.put(entry.entityTypeId().toString(), entry);
            } catch (Exception ex) {
                RagnarMMO.LOGGER.warn("Failed loading internal manual mob entry {}: {}", i, ex.getMessage());
            }
        }
        return data;
    }
}
