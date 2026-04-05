package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.SkillEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectBindingsRegistry;
import com.etema.ragnarmmo.client.effects.SkillEffectRegistry;
import com.etema.ragnarmmo.client.effects.runtime.EffectContext;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.etema.ragnarmmo.client.effects.runtime.EffectManager;
import com.etema.ragnarmmo.client.effects.runtime.EffectPlaybackState;
import com.etema.ragnarmmo.client.effects.runtime.EntityEffectAnchor;
import com.etema.ragnarmmo.client.effects.runtime.SkillEffectRuntimeDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

public final class SkillEntityEffectBridge {
    private SkillEntityEffectBridge() {
    }

    public static boolean render(Entity entity, ResourceLocation skillId, EffectTriggerPhase phase, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (skillId == null) {
            return false;
        }

        List<ResourceLocation> effectIds = SkillEffectBindingsRegistry.resolveWithFallback(skillId, phase);
        if (effectIds.isEmpty()) {
            return false;
        }

        boolean rendered = false;
        for (ResourceLocation effectId : effectIds) {
            SkillEffectDefinition definition = SkillEffectRegistry.get(effectId).orElse(null);
            if (definition == null) {
                continue;
            }

            if (SkillEffectRuntimeDispatcher.requiresManagedLifecycle(definition)) {
                EffectManager.get().ensureAttachedEffect(entity, effectId, EffectContext.builder().build());
                rendered = true;
                continue;
            }

            EffectInstance instance = new EffectInstance(
                    UUID.randomUUID(),
                    definition,
                    new EntityEffectAnchor(entity),
                    EffectContext.builder().build(),
                    new EffectPlaybackState(entity.tickCount));
            SkillEffectRenderDispatcher.renderAtCurrentOrigin(instance, poseStack, bufferSource, packedLight,
                    partialTick);
            rendered = true;
        }
        return rendered;
    }
}
