package com.etema.ragnarmmo.client.effects.runtime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface EffectAnchor {
    Vec3 resolvePosition(float partialTick);

    default boolean isAlive() {
        return true;
    }

    default Entity entity() {
        return null;
    }
}
