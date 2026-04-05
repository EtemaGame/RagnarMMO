package com.etema.ragnarmmo.client.effects.runtime;

import com.etema.ragnarmmo.client.effects.EffectVec3;
import com.etema.ragnarmmo.client.effects.ParticleEmitterEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public final class SkillEffectRuntimeDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectRuntimeDispatcher.class);
    private static final Set<ResourceLocation> WARNED_PARTICLES = new HashSet<>();

    private SkillEffectRuntimeDispatcher() {
    }

    public static boolean requiresManagedLifecycle(SkillEffectDefinition definition) {
        return definition.type() == SkillEffectType.PARTICLE_EMITTER;
    }

    public static void onSpawn(EffectInstance instance, ClientLevel level) {
        if (level == null) {
            return;
        }
        tick(instance, level);
    }

    public static void tick(EffectInstance instance, ClientLevel level) {
        if (level == null) {
            return;
        }

        switch (instance.definition().type()) {
            case PARTICLE_EMITTER ->
                    tickParticleEmitter(instance, (ParticleEmitterEffectDefinition) instance.definition(), level);
            default -> {
            }
        }
    }

    private static void tickParticleEmitter(EffectInstance instance, ParticleEmitterEffectDefinition definition,
            ClientLevel level) {
        EffectPlaybackState playbackState = instance.playbackState();
        int age = playbackState.ageTicks();
        if (playbackState.lastRuntimeExecutionAge() == age) {
            return;
        }

        int interval = Math.max(1, definition.emitIntervalTicks());
        if (age % interval != 0) {
            return;
        }

        ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(definition.particle());
        if (!(particleType instanceof SimpleParticleType simpleParticleType)) {
            warnUnsupportedParticle(definition.particle());
            return;
        }

        playbackState.setLastRuntimeExecutionAge(age);

        Vec3 origin = instance.anchor().resolvePosition(1.0f)
                .add(toVec3(instance.context().offset()))
                .add(toVec3(definition.offset()));
        Vec3 inheritedVelocity = resolveInheritedVelocity(instance.anchor().entity(), definition.inheritEntityVelocity());
        RandomSource random = level.random;
        float scaleMultiplier = instance.context().scaleMultiplier();
        float radius = definition.radius() * scaleMultiplier;
        Vec3 spread = toVec3(definition.spread()).scale(scaleMultiplier);
        Vec3 baseVelocity = toVec3(definition.baseVelocity());
        Vec3 randomVelocity = toVec3(definition.randomVelocity());

        for (int particleIndex = 0; particleIndex < definition.count(); particleIndex++) {
            Vec3 localOffset = sampleLocalOffset(definition, age, particleIndex, random, radius);
            Vec3 jitteredOffset = addRandomJitter(localOffset, spread, random);
            Vec3 velocity = baseVelocity
                    .add(resolveRadialVelocity(definition, localOffset))
                    .add(randomRange(random, randomVelocity.x), randomRange(random, randomVelocity.y),
                            randomRange(random, randomVelocity.z))
                    .add(inheritedVelocity);

            Vec3 particlePos = origin.add(jitteredOffset);
            level.addParticle(simpleParticleType, particlePos.x, particlePos.y, particlePos.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private static Vec3 resolveInheritedVelocity(Entity entity, float inheritScale) {
        if (entity == null || inheritScale == 0.0f) {
            return Vec3.ZERO;
        }
        return entity.getDeltaMovement().scale(inheritScale);
    }

    private static Vec3 resolveRadialVelocity(ParticleEmitterEffectDefinition definition, Vec3 localOffset) {
        if (definition.radialVelocity() == 0.0f || localOffset.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return localOffset.normalize().scale(definition.radialVelocity());
    }

    private static Vec3 sampleLocalOffset(ParticleEmitterEffectDefinition definition, int age, int particleIndex,
            RandomSource random, float radius) {
        return switch (definition.shape()) {
            case POINT -> Vec3.ZERO;
            case RING -> sampleRing(definition, age, particleIndex, radius);
            case SPHERE -> sampleSphere(random, radius);
        };
    }

    private static Vec3 sampleRing(ParticleEmitterEffectDefinition definition, int age, int particleIndex, float radius) {
        if (radius == 0.0f) {
            return Vec3.ZERO;
        }

        float baseAngle = definition.rotationPerTickDegrees() * age;
        float step = 360.0f / Math.max(1, definition.count());
        double angle = Math.toRadians(baseAngle + (step * particleIndex));
        return new Vec3(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

    private static Vec3 sampleSphere(RandomSource random, float radius) {
        if (radius == 0.0f) {
            return Vec3.ZERO;
        }

        Vec3 direction = new Vec3(
                randomRange(random, 1.0f),
                randomRange(random, 1.0f),
                randomRange(random, 1.0f));
        if (direction.lengthSqr() < 1.0E-6) {
            direction = new Vec3(0.0, 1.0, 0.0);
        }
        return direction.normalize().scale(radius);
    }

    private static Vec3 addRandomJitter(Vec3 base, Vec3 spread, RandomSource random) {
        if (spread.lengthSqr() < 1.0E-6) {
            return base;
        }
        return base.add(
                randomRange(random, spread.x),
                randomRange(random, spread.y),
                randomRange(random, spread.z));
    }

    private static Vec3 toVec3(EffectVec3 vec3) {
        return new Vec3(vec3.x(), vec3.y(), vec3.z());
    }

    private static double randomRange(RandomSource random, double amount) {
        if (amount == 0.0f) {
            return 0.0f;
        }
        return (random.nextFloat() * 2.0f - 1.0f) * amount;
    }

    private static void warnUnsupportedParticle(ResourceLocation particleId) {
        if (WARNED_PARTICLES.add(particleId)) {
            LOGGER.warn("Particle emitter currently supports only simple particle types. Skipping {}", particleId);
        }
    }
}
