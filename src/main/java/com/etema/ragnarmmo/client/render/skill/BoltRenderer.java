package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.render.SkillEntityEffectBridge;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.entity.projectile.FireBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.IceBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class BoltRenderer extends EntityRenderer<AbstractMagicProjectile> {
    private static final ResourceLocation FIRE_TEXTURE = vanillaBlockTexture("shroomlight");
    private static final ResourceLocation ICE_TEXTURE = vanillaBlockTexture("blue_ice");
    private static final ResourceLocation LIGHTNING_TEXTURE = vanillaBlockTexture("glowstone");

    public BoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static ResourceLocation vanillaBlockTexture(String name) {
        return new ResourceLocation("minecraft", "textures/block/" + name + ".png");
    }

    @Override
    public void render(AbstractMagicProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        ResourceLocation skillId = getSkillId(entity);
        if (SkillEntityEffectBridge.render(entity, skillId, EffectTriggerPhase.PROJECTILE_TICK, poseStack, buffer,
                packedLight, partialTicks)) {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();

        float lerpYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float lerpPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(lerpYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lerpPitch));
        poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTicks) * 18.0F));

        if (!renderConfiguredLayers(skillId, poseStack, buffer, packedLight)) {
            if (entity instanceof FireBoltProjectile) {
                renderPrism(poseStack, buffer, packedLight, Blocks.MAGMA_BLOCK.defaultBlockState(), 0.22f, 0.22f, 0.95f, 0.0f);
                renderPrism(poseStack, buffer, packedLight, Blocks.SHROOMLIGHT.defaultBlockState(), 0.12f, 0.12f, 1.12f, 45.0f);
            } else if (entity instanceof IceBoltProjectile) {
                renderPrism(poseStack, buffer, packedLight, Blocks.BLUE_ICE.defaultBlockState(), 0.22f, 0.22f, 0.95f, 0.0f);
                renderPrism(poseStack, buffer, packedLight, Blocks.PACKED_ICE.defaultBlockState(), 0.12f, 0.12f, 1.08f, 45.0f);
            } else if (entity instanceof LightningBoltProjectile) {
                renderPrism(poseStack, buffer, packedLight, Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), 0.16f, 0.16f, 1.05f, 0.0f);
                renderPrism(poseStack, buffer, packedLight, Blocks.GLOWSTONE.defaultBlockState(), 0.08f, 0.08f, 1.22f, 45.0f);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private ResourceLocation getSkillId(AbstractMagicProjectile entity) {
        if (entity instanceof FireBoltProjectile) {
            return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_bolt");
        }
        if (entity instanceof IceBoltProjectile) {
            return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "cold_bolt");
        }
        if (entity instanceof LightningBoltProjectile) {
            return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "lightning_bolt");
        }
        return null;
    }

    private boolean renderConfiguredLayers(ResourceLocation skillId, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
        if (skillId == null) {
            return false;
        }

        var layersOpt = SkillMaterialVisualsRegistry.get(skillId);
        if (layersOpt.isEmpty()) {
            return false;
        }

        for (BlockLayerVisual layer : layersOpt.get()) {
            BlockState state = ForgeRegistries.BLOCKS.getValue(layer.blockId()) != null
                    ? ForgeRegistries.BLOCKS.getValue(layer.blockId()).defaultBlockState()
                    : null;
            if (state == null) {
                continue;
            }
            renderLayer(poseStack, buffer, packedLight, state, layer);
        }
        return true;
    }

    private void renderLayer(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BlockState state,
            BlockLayerVisual layer) {
        poseStack.pushPose();
        if (layer.rotationX() != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(layer.rotationX()));
        }
        if (layer.rotationY() != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(layer.rotationY()));
        }
        if (layer.rotationZ() != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(layer.rotationZ()));
        }
        poseStack.scale(layer.scaleX(), layer.scaleY(), layer.scaleZ());
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void renderPrism(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BlockState state,
            float scaleX, float scaleY, float scaleZ, float rollDegrees) {
        poseStack.pushPose();
        if (rollDegrees != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        }
        poseStack.scale(scaleX, scaleY, scaleZ);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
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
