package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.CompositeEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectRegistry;
import com.etema.ragnarmmo.client.effects.runtime.EffectContext;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.etema.ragnarmmo.client.effects.runtime.EffectPlaybackState;
import com.etema.ragnarmmo.client.effects.runtime.WorldEffectAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.UUID;

public final class CompositeEffectRenderer implements EffectRenderer<CompositeEffectDefinition> {
    @Override
    public void render(EffectInstance instance, CompositeEffectDefinition definition, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, float partialTick) {
        for (CompositeEffectDefinition.ChildDefinition child : definition.children()) {
            int childAge = instance.playbackState().ageTicks() - child.startTick();
            if (childAge < 0) {
                continue;
            }

            SkillEffectRegistry.get(child.effectId()).ifPresent(childDefinition -> {
                EffectContext context = EffectContext.builder()
                        .scaleMultiplier(instance.context().scaleMultiplier() * child.scaleMultiplier())
                        .tint(instance.context().tint().multiply(child.tint()))
                        .offset(child.offset())
                        .build();
                EffectInstance childInstance = new EffectInstance(
                        UUID.randomUUID(),
                        childDefinition,
                        new WorldEffectAnchor(instance.anchor().resolvePosition(partialTick)),
                        context,
                        new EffectPlaybackState(childAge));
                SkillEffectRenderDispatcher.renderAtCurrentOrigin(childInstance, poseStack, bufferSource, packedLight,
                        partialTick);
            });
        }
    }
}
