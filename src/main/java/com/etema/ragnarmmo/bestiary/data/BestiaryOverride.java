package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.bestiary.api.BestiaryCategory;
import com.etema.ragnarmmo.bestiary.api.BestiaryDropInfoDto;
import com.etema.ragnarmmo.bestiary.api.BestiarySpawnInfoDto;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record BestiaryOverride(
        ResourceLocation entityId,
        Optional<BestiaryCategory> category,
        boolean visible,
        String descriptionId,
        Optional<BestiarySpawnInfoDto> spawn,
        List<BestiaryDropInfoDto> drops,
        ResourceLocation source) {

    public BestiaryOverride {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        category = category == null ? Optional.empty() : category;
        descriptionId = descriptionId == null ? "" : descriptionId;
        spawn = spawn == null ? Optional.empty() : spawn;
        drops = drops == null ? List.of() : List.copyOf(drops);
    }
}
