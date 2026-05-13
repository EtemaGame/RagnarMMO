package com.etema.ragnarmmo.bestiary.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BestiaryEntryDetailsDto(
        ResourceLocation entityId,
        String descriptionId,
        BestiaryStatPreviewDto stats,
        List<BestiaryDropInfoDto> drops,
        BestiarySpawnInfoDto spawn) {

    public BestiaryEntryDetailsDto {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        descriptionId = descriptionId == null ? "" : descriptionId;
        drops = drops == null ? List.of() : List.copyOf(drops);
    }

    public static void encode(BestiaryEntryDetailsDto dto, FriendlyByteBuf buf) {
        buf.writeResourceLocation(dto.entityId);
        buf.writeUtf(dto.descriptionId);
        buf.writeBoolean(dto.stats != null);
        if (dto.stats != null) {
            BestiaryStatPreviewDto.encode(dto.stats, buf);
        }
        buf.writeVarInt(dto.drops.size());
        for (BestiaryDropInfoDto drop : dto.drops) {
            BestiaryDropInfoDto.encode(drop, buf);
        }
        buf.writeBoolean(dto.spawn != null);
        if (dto.spawn != null) {
            BestiarySpawnInfoDto.encode(dto.spawn, buf);
        }
    }

    public static BestiaryEntryDetailsDto decode(FriendlyByteBuf buf) {
        ResourceLocation entityId = buf.readResourceLocation();
        String descriptionId = buf.readUtf();
        BestiaryStatPreviewDto stats = buf.readBoolean() ? BestiaryStatPreviewDto.decode(buf) : null;
        int dropCount = buf.readVarInt();
        List<BestiaryDropInfoDto> drops = new ArrayList<>(dropCount);
        for (int i = 0; i < dropCount; i++) {
            drops.add(BestiaryDropInfoDto.decode(buf));
        }
        BestiarySpawnInfoDto spawn = buf.readBoolean() ? BestiarySpawnInfoDto.decode(buf) : null;
        return new BestiaryEntryDetailsDto(entityId, descriptionId, stats, drops, spawn);
    }
}
