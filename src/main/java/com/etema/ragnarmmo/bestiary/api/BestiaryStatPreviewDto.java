package com.etema.ragnarmmo.bestiary.api;

import net.minecraft.network.FriendlyByteBuf;

public record BestiaryStatPreviewDto(
        boolean hasAuthoredStats,
        int level,
        String rank,
        String tier,
        String race,
        String element,
        String size,
        int maxHp,
        int atkMin,
        int atkMax,
        int def,
        int mdef,
        boolean runtimeScaling) {

    public BestiaryStatPreviewDto {
        rank = rank == null ? "" : rank;
        tier = tier == null ? "" : tier;
        race = race == null ? "" : race;
        element = element == null ? "" : element;
        size = size == null ? "" : size;
    }

    public static void encode(BestiaryStatPreviewDto dto, FriendlyByteBuf buf) {
        buf.writeBoolean(dto.hasAuthoredStats);
        buf.writeVarInt(dto.level);
        buf.writeUtf(dto.rank);
        buf.writeUtf(dto.tier);
        buf.writeUtf(dto.race);
        buf.writeUtf(dto.element);
        buf.writeUtf(dto.size);
        buf.writeVarInt(dto.maxHp);
        buf.writeVarInt(dto.atkMin);
        buf.writeVarInt(dto.atkMax);
        buf.writeVarInt(dto.def);
        buf.writeVarInt(dto.mdef);
        buf.writeBoolean(dto.runtimeScaling);
    }

    public static BestiaryStatPreviewDto decode(FriendlyByteBuf buf) {
        return new BestiaryStatPreviewDto(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean());
    }
}
