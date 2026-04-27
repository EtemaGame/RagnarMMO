package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.render.SkillEntityEffectBridge;
import com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SoulStrikeRenderer extends EntityRenderer<SoulStrikeProjectile> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/soul_sand.png");

    public SoulStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoulStrikeProjectile entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (SkillEntityEffectBridge.render(entity, ResourceLocation.fromNamespaceAndPath("ragnarmmo", "soul_strike"),
                EffectTriggerPhase.PROJECTILE_TICK, poseStack, bufferSource, light, partialTicks)) {
            super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
            return;
        }

        poseStack.pushPose();
        
        float lerpYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float lerpPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        
        poseStack.mulPose(Axis.YP.rotationDegrees(lerpYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lerpPitch));

        poseStack.scale(0.4f, 0.4f, 0.4f);

        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        
        renderPlane(poseStack, vertexconsumer, light, 0); 
        renderPlane(poseStack, vertexconsumer, light, 90); 
        
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    private void renderPlane(PoseStack poseStack, VertexConsumer consumer, int light, float rollDegrees) {
        poseStack.pushPose();
        if (rollDegrees != 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rollDegrees));
        }
        
        PoseStack.Pose last = poseStack.last();
        Matrix4f matrix4f = last.pose();
        Matrix3f matrix3f = last.normal();

        drawVertex(matrix4f, matrix3f, consumer, 0.0f, -1.0f, -1.0f, 0, 1, light);
        drawVertex(matrix4f, matrix3f, consumer, 0.0f, -1.0f, 1.0f, 1, 1, light);
        drawVertex(matrix4f, matrix3f, consumer, 0.0f, 1.0f, 1.0f, 1, 0, light);
        drawVertex(matrix4f, matrix3f, consumer, 0.0f, 1.0f, -1.0f, 0, 0, light);
        
        poseStack.popPose();
    }

    private void drawVertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z, float u, float v, int light) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 1.0F, 0.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SoulStrikeProjectile entity) {
        return TEXTURE;
    }
}
