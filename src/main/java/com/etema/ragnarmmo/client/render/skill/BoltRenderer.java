package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.entity.projectile.FireBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.IceBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BoltRenderer extends EntityRenderer<AbstractMagicProjectile> {
    private static final ResourceLocation FIRE_TEXTURE = new ResourceLocation("ragnarmmo",
            "textures/entity/skill/fire_bolt.png");
    private static final ResourceLocation ICE_TEXTURE = new ResourceLocation("ragnarmmo",
            "textures/entity/skill/ice_bolt.png");
    private static final ResourceLocation LIGHTNING_TEXTURE = new ResourceLocation("ragnarmmo",
            "textures/entity/skill/lightning_bolt.png");

    public BoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AbstractMagicProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Face the player (billboard)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer vertexConsumer = buffer
                .getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));

        // RO animations usually cycle through a sprite sheet.
        // User's specific case: 3 columns, 2 rows = 6 frames.
        int columns = 3;
        int rows = 2;
        int totalFrames = 6;
        float size = 0.5f;

        // Use AnimatedSpriteRenderer to calculate UVs for the current frame
        AnimatedSpriteRenderer.renderFrame(poseStack, vertexConsumer, packedLight, entity.tickCount, columns, rows,
                totalFrames, size);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMagicProjectile entity) {
        if (entity instanceof FireBoltProjectile)
            return FIRE_TEXTURE;
        if (entity instanceof IceBoltProjectile)
            return ICE_TEXTURE;
        if (entity instanceof LightningBoltProjectile)
            return LIGHTNING_TEXTURE;
        return FIRE_TEXTURE;
    }
}
