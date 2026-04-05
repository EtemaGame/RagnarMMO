package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("ragnarmmo", "magic_projectile"), "main");

    // Simplified flattened texture paths
    private static final String BASE_PATH = "textures/entity/magic/";
    private static final ResourceLocation FIREBALL_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "fireball_1.png");
    private static final ResourceLocation FIREBOLT_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "firebolt_1.png");
    private static final ResourceLocation ICEBOLT_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "icebolt_1.png");
    private static final ResourceLocation LIGHTNINGBOLT_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "lightningbolt_1.png");
    private static final ResourceLocation EARTH_SPIKE_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "earth_spike_1.png");
    private static final ResourceLocation FIREWALL_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "firewall_1.png");
    private static final ResourceLocation FROST_NOVA_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "frost_nova_1.png");
    private static final ResourceLocation HEAVENS_DRIVE_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "heavens_drive_1.png");
    private static final ResourceLocation ICEWALL_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "icewall_1.png");
    private static final ResourceLocation JUPITEL_THUNDER_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "jupitelthunder_1.png");
    private static final ResourceLocation METEOR_STORM_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "meteor_storm_1.png");
    private static final ResourceLocation QUAGMIRE_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "quagmire.png");
    private static final ResourceLocation STORM_GUST_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "stormgust_1.png");
    private static final ResourceLocation THUNDER_STORM_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "thunderstorm_1.png");
    private static final ResourceLocation WATERBALL_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "waterball_1.png");
    private static final ResourceLocation SOUL_STRIKE_TEX = new ResourceLocation("ragnarmmo", BASE_PATH + "soul_strike_1.png");

    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
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

        ResourceLocation texture = getProjectileTexture(type);

        poseStack.pushPose();
        
        if ("fireball".equals(type)) {
            // RO Fireball: Billboard Sprite (always faces player)
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            
            float size = 0.8f;
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(texture));
            
            AnimatedSpriteRenderer.renderFrame(poseStack, vertexconsumer, light, entity.tickCount, 3, 2, 6, size);
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
            default -> FIREBOLT_TEX;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectileEntity entity) {
        return getProjectileTexture(entity.getProjectileType());
    }
}
