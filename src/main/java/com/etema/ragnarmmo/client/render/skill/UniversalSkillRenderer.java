package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.render.SkillEntityEffectBridge;
import com.etema.ragnarmmo.entity.IVisualSkillEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Generic renderer for skills that uses SkillVisuals for data-driven rendering.
 */
public class UniversalSkillRenderer<T extends Entity> extends EntityRenderer<T> {

    public UniversalSkillRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity instanceof IVisualSkillEntity visualEntity) {
            if (SkillEntityEffectBridge.render(entity, visualEntity.getSkillId(), EffectTriggerPhase.AOE_LOOP, poseStack,
                    bufferSource, packedLight, partialTicks)) {
                super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
                return;
            }
            SkillVisualsRegistry.get(visualEntity.getSkillId()).ifPresent(visuals -> {
            poseStack.pushPose();
            
            // Basic centering - higher for billboard, lower for floor effects
            if (visuals.billboard()) {
                poseStack.translate(0, 0.5, 0);
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            } else {
                // Horizontal sprite for "floor" effects like Warp Portal
                poseStack.translate(0, 0.02, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }

            poseStack.scale(visuals.size(), visuals.size(), visuals.size());

            switch (visuals.type()) {
                case SPRITE -> renderSprite(entity, visuals, poseStack, bufferSource, packedLight);
                case BLOCK -> renderBlock(entity, visuals, poseStack, bufferSource, packedLight);
                case MODEL -> renderModel(entity, visuals, poseStack, bufferSource, packedLight);
            }

            poseStack.popPose();
        });
        }
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderSprite(T entity, SkillVisuals visuals, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        VertexConsumer consumer = bufferSource.getBuffer(visuals.renderType());
        int ticks = entity.tickCount;
        int totalFrames = visuals.columns() * visuals.rows();
        
        AnimatedSpriteRenderer.renderFrame(
            poseStack, consumer, light, 
            ticks, visuals.columns(), visuals.rows(), totalFrames, 1.0f
        );
    }

    private void renderBlock(T entity, SkillVisuals visuals, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        visuals.blockState().ifPresent(state -> {
            poseStack.translate(-0.5, -0.5, -0.5); // Center block
            net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, poseStack, bufferSource, light, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.ModelData.EMPTY,
                null
            );
        });
    }

    private void renderModel(T entity, SkillVisuals visuals, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (entity instanceof IVisualSkillEntity visualEntity) {
            return SkillVisualsRegistry.get(visualEntity.getSkillId())
                    .map(SkillVisuals::texture)
                    .orElse(ResourceLocation.fromNamespaceAndPath("minecraft", "missingno"));
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
    }
}
