package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.SkillEffectDefinition;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public interface EffectRenderer<T extends SkillEffectDefinition> {
    void render(EffectInstance instance, T definition, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, float partialTick);
}
