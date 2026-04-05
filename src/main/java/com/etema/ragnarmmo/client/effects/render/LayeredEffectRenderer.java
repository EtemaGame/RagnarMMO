package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.EffectColor;
import com.etema.ragnarmmo.client.effects.LayeredEffectDefinition;
import com.etema.ragnarmmo.client.effects.runtime.EffectInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LayeredEffectRenderer implements EffectRenderer<LayeredEffectDefinition> {
    @Override
    public void render(EffectInstance instance, LayeredEffectDefinition definition, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, float partialTick) {
        List<LayeredEffectDefinition.LayerDefinition> orderedLayers = new ArrayList<>(definition.layers());
        orderedLayers.sort(Comparator.comparingInt(LayeredEffectDefinition.LayerDefinition::zOrder));

        for (LayeredEffectDefinition.LayerDefinition layer : orderedLayers) {
            LayeredEffectDefinition.KeyframeDefinition keyframe = sample(layer, instance, partialTick);
            if (keyframe == null || keyframe.texture() == null) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(keyframe.x(), keyframe.y(), keyframe.z());
            SkillEffectRenderDispatcher.applyOrientation(poseStack, layer.orientation(), instance);
            poseStack.mulPose(Axis.ZP.rotationDegrees(keyframe.rotationDeg()));
            poseStack.translate(0.0f, 0.0f, layer.zOrder() * 0.001f);

            EffectColor color = keyframe.color().multiply(instance.context().tint());
            float halfWidth = keyframe.scaleX() * instance.context().scaleMultiplier();
            float halfHeight = keyframe.scaleY() * instance.context().scaleMultiplier();
            VertexConsumer consumer = bufferSource.getBuffer(EffectRenderTypes.resolve(layer.blendMode(),
                    keyframe.texture()));

            EffectQuadRenderer.renderQuad(
                    poseStack,
                    consumer,
                    packedLight,
                    halfWidth,
                    halfHeight,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f,
                    toChannel(color.r()),
                    toChannel(color.g()),
                    toChannel(color.b()),
                    toChannel(color.a()));
            poseStack.popPose();
        }
    }

    private LayeredEffectDefinition.KeyframeDefinition sample(LayeredEffectDefinition.LayerDefinition layer,
            EffectInstance instance, float partialTick) {
        List<LayeredEffectDefinition.KeyframeDefinition> keyframes = layer.keyframes();
        if (keyframes.isEmpty()) {
            return null;
        }

        float tick = instance.playbackState().ageTicks() + partialTick;
        LayeredEffectDefinition.KeyframeDefinition previous = keyframes.get(0);
        LayeredEffectDefinition.KeyframeDefinition next = keyframes.get(keyframes.size() - 1);

        for (LayeredEffectDefinition.KeyframeDefinition current : keyframes) {
            if (current.tick() <= tick) {
                previous = current;
            }
            if (current.tick() >= tick) {
                next = current;
                break;
            }
        }

        if (previous == next || next.tick() == previous.tick()) {
            return previous;
        }

        float lerp = (tick - previous.tick()) / (float) (next.tick() - previous.tick());
        return new LayeredEffectDefinition.KeyframeDefinition(
                Math.round(tick),
                lerp(previous.x(), next.x(), lerp),
                lerp(previous.y(), next.y(), lerp),
                lerp(previous.z(), next.z(), lerp),
                lerp(previous.scaleX(), next.scaleX(), lerp),
                lerp(previous.scaleY(), next.scaleY(), lerp),
                lerp(previous.rotationDeg(), next.rotationDeg(), lerp),
                lerp(previous.alpha(), next.alpha(), lerp),
                lerp(previous.r(), next.r(), lerp),
                lerp(previous.g(), next.g(), lerp),
                lerp(previous.b(), next.b(), lerp),
                previous.texture() != null ? previous.texture() : next.texture());
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private int toChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }
}
