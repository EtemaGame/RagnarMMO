package com.etema.ragnarmmo.client.render.skill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Utility for rendering animated sprite sheets (grid based).
 */
public class AnimatedSpriteRenderer {

    /**
     * Renders a single frame from a sprite sheet grid.
     */
    public static void renderFrame(PoseStack poseStack, VertexConsumer consumer, int light, 
                                   int tickCount, int columns, int rows, int totalFrames, float size) {
        
        int currentFrame = tickCount % totalFrames;
        int col = currentFrame % columns;
        int row = currentFrame / columns;

        float uMin = (float) col / columns;
        float uMax = (float) (col + 1) / columns;
        float vMin = (float) row / rows;
        float vMax = (float) (row + 1) / rows;

        Matrix4f matrix = poseStack.last().pose();

        // Vertex 1 (Bottom-Left)
        consumer.vertex(matrix, -size, -size, 0)
                .color(255, 255, 255, 255)
                .uv(uMin, vMax)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
        
        // Vertex 2 (Bottom-Right)
        consumer.vertex(matrix, size, -size, 0)
                .color(255, 255, 255, 255)
                .uv(uMax, vMax)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
        
        // Vertex 3 (Top-Right)
        consumer.vertex(matrix, size, size, 0)
                .color(255, 255, 255, 255)
                .uv(uMax, vMin)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
        
        // Vertex 4 (Top-Left)
        consumer.vertex(matrix, -size, size, 0)
                .color(255, 255, 255, 255)
                .uv(uMin, vMin)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }
}
