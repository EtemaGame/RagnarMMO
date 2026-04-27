package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.render.SkillEntityEffectBridge;
import com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "magic_projectile"), "main");

    // Current visuals are vanilla-first. Future custom textures can be restored by
    // swapping these paths or by using explicit data-driven asset declarations.
    private static final ResourceLocation FIREBALL_TEX = vanillaBlockTexture("magma");
    private static final ResourceLocation FIREBOLT_TEX = vanillaBlockTexture("shroomlight");
    private static final ResourceLocation ICEBOLT_TEX = vanillaBlockTexture("blue_ice");
    private static final ResourceLocation LIGHTNINGBOLT_TEX = vanillaBlockTexture("glowstone");
    private static final ResourceLocation EARTH_SPIKE_TEX = vanillaBlockTexture("stone");
    private static final ResourceLocation FIREWALL_TEX = vanillaBlockTexture("fire_0");
    private static final ResourceLocation FROST_NOVA_TEX = vanillaBlockTexture("packed_ice");
    private static final ResourceLocation HEAVENS_DRIVE_TEX = vanillaBlockTexture("stone");
    private static final ResourceLocation ICEWALL_TEX = vanillaBlockTexture("packed_ice");
    private static final ResourceLocation JUPITEL_THUNDER_TEX = vanillaBlockTexture("glowstone");
    private static final ResourceLocation METEOR_STORM_TEX = vanillaBlockTexture("magma");
    private static final ResourceLocation QUAGMIRE_TEX = vanillaBlockTexture("mud");
    private static final ResourceLocation STORM_GUST_TEX = vanillaBlockTexture("packed_ice");
    private static final ResourceLocation THUNDER_STORM_TEX = vanillaBlockTexture("glowstone");
    private static final ResourceLocation WATERBALL_TEX = vanillaBlockTexture("prismarine");
    private static final ResourceLocation SOUL_STRIKE_TEX = vanillaBlockTexture("soul_sand");

    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static ResourceLocation vanillaBlockTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/" + name + ".png");
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition createBodyLayer() {
        net.minecraft.client.model.geom.builders.MeshDefinition meshdefinition = new net.minecraft.client.model.geom.builders.MeshDefinition();
        return net.minecraft.client.model.geom.builders.LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void render(MagicProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        String type = entity.getProjectileType();
        if ("none".equals(type) || "default".equals(type)) {
            super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
            return;
        }

        if (SkillEntityEffectBridge.render(entity, entity.getSkillId(), EffectTriggerPhase.PROJECTILE_TICK, poseStack,
                bufferSource, light, partialTicks)) {
            super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
            return;
        }

        ResourceLocation texture = getProjectileTexture(type);

        poseStack.pushPose();
        
        if ("fireball".equals(type)) {
            poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 14.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTicks) * 9.0F));
            if (!renderConfiguredLayers(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_ball"), poseStack, bufferSource, light)) {
                renderBlockCore(poseStack, bufferSource, light, Blocks.MAGMA_BLOCK.defaultBlockState(), 0.82f, 0.82f, 0.82f, 0.0f);
                renderBlockCore(poseStack, bufferSource, light, Blocks.SHROOMLIGHT.defaultBlockState(), 0.52f, 0.52f, 0.52f, 45.0f);
            }
        } else if ("holy_light".equals(type)) {
            float lerpYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float lerpPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

            poseStack.mulPose(Axis.YP.rotationDegrees(lerpYaw - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(lerpPitch));
            poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTicks) * 22.0F));

            if (!renderConfiguredLayers(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "holy_light"), poseStack, bufferSource, light)) {
                renderBlockCore(poseStack, bufferSource, light, Blocks.SEA_LANTERN.defaultBlockState(), 0.18f, 0.18f, 1.05f, 0.0f);
                renderBlockCore(poseStack, bufferSource, light, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 0.10f, 0.10f, 1.22f, 45.0f);
            }
        } else {
            // Other skills: 3D "Arrow" Style Model
            float lerpYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float lerpPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            
            poseStack.mulPose(Axis.YP.rotationDegrees(lerpYaw - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(lerpPitch));

            float size = 0.4f;
            poseStack.scale(size, size, size);

            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
            
            renderPlane(poseStack, vertexconsumer, light, 0); 
            renderPlane(poseStack, vertexconsumer, light, 90); 
        }
        
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    private boolean renderConfiguredLayers(ResourceLocation skillId, PoseStack poseStack, MultiBufferSource bufferSource,
            int light) {
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
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, light,
                    OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
            poseStack.popPose();
        }

        return true;
    }

    private void renderBlockCore(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlockState state,
            float scaleX, float scaleY, float scaleZ, float yRotation) {
        poseStack.pushPose();
        if (yRotation != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        }
        poseStack.scale(scaleX, scaleY, scaleZ);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, light,
                OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
        poseStack.popPose();
    }

    private void renderPlane(PoseStack poseStack, VertexConsumer consumer, int light, float rollDegrees) {
        poseStack.pushPose();
        if (rollDegrees != 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rollDegrees));
        }
        
        PoseStack.Pose posestack$pose = poseStack.last();
        Matrix4f matrix4f = posestack$pose.pose();
        Matrix3f matrix3f = posestack$pose.normal();

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

    private ResourceLocation getProjectileTexture(String type) {
        return switch (type) {
            case "fireball" -> FIREBALL_TEX;
            case "firebolt" -> FIREBOLT_TEX;
            case "icebolt" -> ICEBOLT_TEX;
            case "lightningbolt" -> LIGHTNINGBOLT_TEX;
            case "earth_spike" -> EARTH_SPIKE_TEX;
            case "firewall" -> FIREWALL_TEX;
            case "frost_nova" -> FROST_NOVA_TEX;
            case "heavens_drive" -> HEAVENS_DRIVE_TEX;
            case "icewall" -> ICEWALL_TEX;
            case "jupitel_thunder" -> JUPITEL_THUNDER_TEX;
            case "meteor_storm" -> METEOR_STORM_TEX;
            case "quagmire" -> QUAGMIRE_TEX;
            case "soul_strike" -> SOUL_STRIKE_TEX;
            case "napalm_beat" -> SOUL_STRIKE_TEX; 
            case "storm_gust" -> STORM_GUST_TEX;
            case "thunder_storm" -> THUNDER_STORM_TEX;
            case "waterball" -> WATERBALL_TEX;
            default -> SOUL_STRIKE_TEX;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectileEntity entity) {
        return getProjectileTexture(entity.getProjectileType());
    }
}
