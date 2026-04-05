package com.etema.ragnarmmo.client.effects.runtime;

import com.etema.ragnarmmo.client.effects.EffectColor;
import com.etema.ragnarmmo.client.effects.EffectVec3;
import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.SkillEffectBindingsRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SkillEffectSpawner {
    private SkillEffectSpawner() {
    }

    public static Optional<EffectInstance> spawnAttachedEffect(Entity entity, ResourceLocation effectId) {
        return EffectManager.get().spawnAttachedEffect(entity, effectId, EffectContext.builder().build());
    }

    public static Optional<EffectInstance> spawnProjectileEffect(Entity projectile, ResourceLocation effectId) {
        return EffectManager.get().spawnAttachedEffect(projectile, effectId, EffectContext.builder().build());
    }

    public static Optional<EffectInstance> spawnWorldEffect(Vec3 position, ResourceLocation effectId) {
        return EffectManager.get().spawnWorldEffect(position, effectId, EffectContext.builder().build());
    }

    public static Optional<EffectInstance> spawnImpactEffect(Vec3 position, Vec3 normal, ResourceLocation effectId) {
        return EffectManager.get().spawnWorldEffect(position, effectId, EffectContext.builder()
                .normal(new EffectVec3((float) normal.x, (float) normal.y, (float) normal.z))
                .tint(EffectColor.WHITE)
                .build());
    }

    public static List<EffectInstance> spawnAttachedPhaseEffects(Entity entity, ResourceLocation skillId,
            EffectTriggerPhase phase) {
        return spawnAttachedPhaseEffects(entity, skillId, phase, EffectContext.builder().build());
    }

    public static List<EffectInstance> spawnAttachedPhaseEffects(Entity entity, ResourceLocation skillId,
            EffectTriggerPhase phase, EffectContext context) {
        List<EffectInstance> spawned = new ArrayList<>();
        for (ResourceLocation effectId : SkillEffectBindingsRegistry.resolveWithFallback(skillId, phase)) {
            EffectManager.get().spawnAttachedEffect(entity, effectId, context).ifPresent(spawned::add);
        }
        return List.copyOf(spawned);
    }

    public static List<EffectInstance> spawnWorldPhaseEffects(Vec3 position, ResourceLocation skillId,
            EffectTriggerPhase phase) {
        return spawnWorldPhaseEffects(position, skillId, phase, EffectContext.builder().build());
    }

    public static List<EffectInstance> spawnWorldPhaseEffects(Vec3 position, ResourceLocation skillId,
            EffectTriggerPhase phase, EffectContext context) {
        List<EffectInstance> spawned = new ArrayList<>();
        for (ResourceLocation effectId : SkillEffectBindingsRegistry.resolveWithFallback(skillId, phase)) {
            EffectManager.get().spawnWorldEffect(position, effectId, context).ifPresent(spawned::add);
        }
        return List.copyOf(spawned);
    }
}
