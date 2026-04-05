package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

public record ParticleEmitterEffectDefinition(
        ResourceLocation id,
        int durationTicks,
        boolean loop,
        ResourceLocation particle,
        ParticleEmitterShape shape,
        int emitIntervalTicks,
        int count,
        float radius,
        float radialVelocity,
        float rotationPerTickDegrees,
        float inheritEntityVelocity,
        EffectVec3 offset,
        EffectVec3 spread,
        EffectVec3 baseVelocity,
        EffectVec3 randomVelocity) implements SkillEffectDefinition {

    @Override
    public SkillEffectType type() {
        return SkillEffectType.PARTICLE_EMITTER;
    }
}
