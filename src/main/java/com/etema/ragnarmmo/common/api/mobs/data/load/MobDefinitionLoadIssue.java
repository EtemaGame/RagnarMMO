package com.etema.ragnarmmo.common.api.mobs.data.load;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Load-time diagnostic for authored mob datapack resources.
 */
public record MobDefinitionLoadIssue(
        Kind kind,
        ResourceLocation sourceId,
        @Nullable ResourceLocation entityTypeId,
        String message) {

    public enum Kind {
        INVALID,
        INCOMPLETE,
        DUPLICATE
    }
}
