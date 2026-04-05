package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.CompositeEffectDefinition;
import com.etema.ragnarmmo.client.effects.EffectOrientation;
import com.etema.ragnarmmo.client.effects.EffectVec3;
import com.etema.ragnarmmo.client.effects.LayeredEffectDefinition;
import com.etema.ragnarmmo.client.effects.ParticleEmitterEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectDefinition;
import com.etema.ragnarmmo.client.effects.SkillEffectType;
import com.etema.ragnarmmo.client.effects.SpriteSheetEffectDefinition;
import com.etema.ragnarmmo.client.effects.StrLayeredEffectDefinition;
import com.etema.ragnarmmo.client.effects.runtime.EffectContext;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SkillEffectRenderDispatcher {
    private static final SpriteSheetEffectRenderer SPRITE_SHEET_RENDERER = new SpriteSheetEffectRenderer();
    private static final LayeredEffectRenderer LAYERED_RENDERER = new LayeredEffectRenderer();
    private static final CompositeEffectRenderer COMPOSITE_RENDERER = new CompositeEffectRenderer();

    private SkillEffectRenderDispatcher() {
    }

    public static void renderAtCurrentOrigin(EffectInstance instance, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, float partialTick) {
        SkillEffectDefinition definition = instance.definition();
        poseStack.pushPose();

        applyContextOffset(poseStack, instance.context());
        applyDefinitionOffset(poseStack, definition);
        applyOrientation(poseStack, definitionOrientation(definition), instance);

        switch (definition.type()) {
            case SPRITE_SHEET ->
                    SPRITE_SHEET_RENDERER.render(instance, (SpriteSheetEffectDefinition) definition, poseStack,
                            bufferSource, packedLight, partialTick);
            case LAYERED ->
                    LAYERED_RENDERER.render(instance, (LayeredEffectDefinition) definition, poseStack, bufferSource,
                            packedLight, partialTick);
            case STR_LAYERED ->
                    LAYERED_RENDERER.render(instance, ((StrLayeredEffectDefinition) definition).layeredDefinition(),
                            poseStack, bufferSource, packedLight, partialTick);
            case COMPOSITE ->
                    COMPOSITE_RENDERER.render(instance, (CompositeEffectDefinition) definition, poseStack,
                            bufferSource, packedLight, partialTick);
            case PARTICLE_EMITTER -> {
            }
            default -> {
            }
        }

        poseStack.popPose();
    }

    private static void applyContextOffset(PoseStack poseStack, EffectContext context) {
        EffectVec3 offset = context.offset();
        poseStack.translate(offset.x(), offset.y(), offset.z());
    }

    private static void applyDefinitionOffset(PoseStack poseStack, SkillEffectDefinition definition) {
        if (definition instanceof SpriteSheetEffectDefinition spriteDefinition) {
            EffectVec3 offset = spriteDefinition.offset();
            poseStack.translate(offset.x(), offset.y(), offset.z());
        }
    }

    static EffectOrientation definitionOrientation(SkillEffectDefinition definition) {
        return switch (definition.type()) {
            case SPRITE_SHEET -> ((SpriteSheetEffectDefinition) definition).orientation();
            case LAYERED -> EffectOrientation.FIXED;
            case STR_LAYERED -> EffectOrientation.FIXED;
            case COMPOSITE -> EffectOrientation.FIXED;
            case PARTICLE_EMITTER -> EffectOrientation.FIXED;
            default -> EffectOrientation.FIXED;
        };
    }

    static void applyOrientation(PoseStack poseStack, EffectOrientation orientation, EffectInstance instance) {
        Entity entity = instance.anchor().entity();
        switch (orientation) {
            case BILLBOARD -> {
                poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            }
            case GROUND -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            case VELOCITY -> {
                if (entity != null) {
                    Vec3 movement = entity.getDeltaMovement();
                    double horizontalDist = movement.horizontalDistance();
                    float yaw = instance.context().yawOverride() != null
                            ? instance.context().yawOverride()
                            : (float) (Math.atan2(movement.x, movement.z) * (180.0D / Math.PI));
                    float pitch = instance.context().pitchOverride() != null
                            ? instance.context().pitchOverride()
                            : (float) (Math.atan2(movement.y, horizontalDist) * (180.0D / Math.PI));
                    poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
                }
            }
            case FIXED -> {
            }
        }
    }
}
