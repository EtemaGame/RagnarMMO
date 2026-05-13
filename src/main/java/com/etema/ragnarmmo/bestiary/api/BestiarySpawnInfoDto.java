package com.etema.ragnarmmo.bestiary.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BestiarySpawnInfoDto(List<ResourceLocation> dimensions, String notes) {
    public BestiarySpawnInfoDto {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        notes = notes == null ? "" : notes;
    }

    public static void encode(BestiarySpawnInfoDto dto, FriendlyByteBuf buf) {
        buf.writeVarInt(dto.dimensions.size());
        for (ResourceLocation dimension : dto.dimensions) {
            buf.writeResourceLocation(dimension);
        }
        buf.writeUtf(dto.notes);
    }

    public static BestiarySpawnInfoDto decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ResourceLocation> dimensions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            dimensions.add(buf.readResourceLocation());
        }
        return new BestiarySpawnInfoDto(dimensions, buf.readUtf());
    }
}
