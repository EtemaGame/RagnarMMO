package com.etema.ragnarmmo.bestiary.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record BestiaryEntryDto(
        ResourceLocation entityId,
        String modId,
        BestiaryCategory category,
        String descriptionId) {

    public BestiaryEntryDto {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        modId = modId == null ? "" : modId;
        category = category == null ? BestiaryCategory.UNKNOWN : category;
        descriptionId = descriptionId == null ? "" : descriptionId;
    }

    public static void encode(BestiaryEntryDto dto, FriendlyByteBuf buf) {
        buf.writeResourceLocation(dto.entityId);
        buf.writeUtf(dto.modId);
        buf.writeEnum(dto.category);
        buf.writeUtf(dto.descriptionId);
    }

    public static BestiaryEntryDto decode(FriendlyByteBuf buf) {
        return new BestiaryEntryDto(
                buf.readResourceLocation(),
                buf.readUtf(),
                buf.readEnum(BestiaryCategory.class),
                buf.readUtf());
    }
}
