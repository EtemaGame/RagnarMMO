package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.entity.effect.StatusOverlayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class StatusOverlayRenderer extends EntityRenderer<StatusOverlayEntity> {

    public StatusOverlayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StatusOverlayEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, entity.getVisualHeight() * 0.5, 0.0);

        renderShell(poseStack, bufferSource, packedLight, entity.getVariant().outerState(),
                entity.getVisualWidth() * 1.05f, entity.getVisualHeight() * 1.02f, entity.getVisualWidth() * 1.05f, 0.0f);

        renderShell(poseStack, bufferSource, packedLight, entity.getVariant().innerState(),
                entity.getVisualWidth() * 0.88f, entity.getVisualHeight() * 0.94f, entity.getVisualWidth() * 0.88f, 35.0f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderShell(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, BlockState state,
            float scaleX, float scaleY, float scaleZ, float yRotation) {
        poseStack.pushPose();
        if (yRotation != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        }
        poseStack.scale(scaleX, scaleY, scaleZ);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight,
                OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StatusOverlayEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    }
}
