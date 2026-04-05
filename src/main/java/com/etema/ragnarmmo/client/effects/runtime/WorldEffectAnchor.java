package com.etema.ragnarmmo.client.effects.runtime;

import net.minecraft.world.phys.Vec3;

public record WorldEffectAnchor(Vec3 position) implements EffectAnchor {
    @Override
    public Vec3 resolvePosition(float partialTick) {
        return position;
    }
}
