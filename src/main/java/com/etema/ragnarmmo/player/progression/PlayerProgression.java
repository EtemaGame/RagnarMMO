package com.etema.ragnarmmo.player.progression;

import net.minecraft.resources.ResourceLocation;

public record PlayerProgression(
        int baseLevel,
        long baseExp,
        int jobLevel,
        long jobExp,
        int statPoints,
        int skillPoints,
        ResourceLocation jobId) {
}
