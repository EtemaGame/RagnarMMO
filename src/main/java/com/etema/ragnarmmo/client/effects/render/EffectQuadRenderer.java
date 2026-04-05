package com.etema.ragnarmmo.client.effects.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

public final class EffectQuadRenderer {
    private EffectQuadRenderer() {
    }

    public static void renderQuad(PoseStack poseStack, VertexConsumer consumer, int light,
            float halfWidth, float halfHeight,
            float uMin, float uMax, float vMin, float vMax,
            int red, int green, int blue, int alpha) {
        Matrix4f matrix = poseStack.last().pose();

        consumer.vertex(matrix, -halfWidth, -halfHeight, 0.0f)
                .color(red, green, blue, alpha)
                .uv(uMin, vMax)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
        consumer.vertex(matrix, halfWidth, -halfHeight, 0.0f)
                .color(red, green, blue, alpha)
                .uv(uMax, vMax)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
        consumer.vertex(matrix, halfWidth, halfHeight, 0.0f)
                .color(red, green, blue, alpha)
                .uv(uMax, vMin)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
        consumer.vertex(matrix, -halfWidth, halfHeight, 0.0f)
                .color(red, green, blue, alpha)
                .uv(uMin, vMin)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
    }
}
