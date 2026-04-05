package com.etema.ragnarmmo.client.effects.runtime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record EntityEffectAnchor(Entity entity) implements EffectAnchor {
    @Override
    public Vec3 resolvePosition(float partialTick) {
        return entity.getPosition(partialTick);
    }

    @Override
    public boolean isAlive() {
        return entity.isAlive() && !entity.isRemoved();
    }
}
