package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.EffectColor;
import com.etema.ragnarmmo.client.effects.SpriteSheetEffectDefinition;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

public final class SpriteSheetEffectRenderer implements EffectRenderer<SpriteSheetEffectDefinition> {
    @Override
    public void render(EffectInstance instance, SpriteSheetEffectDefinition definition, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, float partialTick) {
        int frameCount = Math.max(1, definition.frameCount());
        int columns = Math.max(1, definition.columns());
        int rows = Math.max(1, definition.rows());
        int fps = Math.max(1, definition.framesPerSecond());
        int elapsedFrames = (int) Math.floor((instance.playbackState().ageTicks() + partialTick) * fps / 20.0f);
        int currentFrame = definition.loop() ? (elapsedFrames % frameCount) : Math.min(elapsedFrames, frameCount - 1);
        int col = currentFrame % columns;
        int row = currentFrame / columns;

        float uMin = (float) col / columns;
        float uMax = (float) (col + 1) / columns;
        float vMin = (float) row / rows;
        float vMax = (float) (row + 1) / rows;

        EffectColor color = instance.context().tint();
        VertexConsumer consumer = bufferSource.getBuffer(EffectRenderTypes.resolve(definition.blendMode(),
                definition.texture()));
        float halfSize = definition.size() * instance.context().scaleMultiplier();

        EffectQuadRenderer.renderQuad(poseStack, consumer, packedLight, halfSize, halfSize, uMin, uMax, vMin, vMax,
                toChannel(color.r()), toChannel(color.g()), toChannel(color.b()), toChannel(color.a()));
    }

    private int toChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }
}
