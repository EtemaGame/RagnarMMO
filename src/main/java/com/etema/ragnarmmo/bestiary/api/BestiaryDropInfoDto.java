package com.etema.ragnarmmo.bestiary.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record BestiaryDropInfoDto(
        ResourceLocation itemId,
        int min,
        int max,
        double chance,
        BestiaryDropSource source,
        String label,
        String noteId) {

    public BestiaryDropInfoDto {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        min = Math.max(0, min);
        max = Math.max(min, max);
        chance = Math.max(0.0D, Math.min(1.0D, chance));
        source = source == null ? BestiaryDropSource.UNKNOWN : source;
        label = label == null ? "" : label;
        noteId = noteId == null ? "" : noteId;
    }

    public static void encode(BestiaryDropInfoDto dto, FriendlyByteBuf buf) {
        buf.writeResourceLocation(dto.itemId);
        buf.writeVarInt(dto.min);
        buf.writeVarInt(dto.max);
        buf.writeDouble(dto.chance);
        buf.writeEnum(dto.source);
        buf.writeUtf(dto.label);
        buf.writeUtf(dto.noteId);
    }

    public static BestiaryDropInfoDto decode(FriendlyByteBuf buf) {
        return new BestiaryDropInfoDto(
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readEnum(BestiaryDropSource.class),
                buf.readUtf(),
                buf.readUtf());
    }
}
