package com.etema.ragnarmmo.bestiary.data;

import net.minecraft.resources.ResourceLocation;

public record BestiaryLoadIssue(
        Kind kind,
        ResourceLocation source,
        ResourceLocation entityId,
        String message) {

    public enum Kind {
        INVALID,
        DUPLICATE,
        MISSING_REGISTRY_ENTRY
    }
}
