package com.etema.ragnarmmo.client.effects.importer;

import com.etema.ragnarmmo.client.effects.BlendMode;
import com.etema.ragnarmmo.client.effects.EffectOrientation;
import com.etema.ragnarmmo.client.effects.LayeredEffectDefinition;
import com.etema.ragnarmmo.client.effects.StrLayeredEffectDefinition;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RoStrAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoStrAdapter.class);

    public StrLayeredEffectDefinition adapt(
            ResourceLocation id,
            RoStrParser.RoStrEffect effect,
            ResourceLocation textureNamespace,
            String textureBasePath,
            EffectOrientation orientation,
            int durationOverrideTicks,
            boolean loop) {
        List<String> warnings = new ArrayList<>();
        List<LayeredEffectDefinition.LayerDefinition> layers = new ArrayList<>();

        for (int layerIndex = 0; layerIndex < effect.layers().size(); layerIndex++) {
            RoStrParser.RoStrEffect.Layer layer = effect.layers().get(layerIndex);
            BlendMode blendMode = resolveBlendMode(layer, warnings, id, layerIndex);
            List<LayeredEffectDefinition.KeyframeDefinition> keyframes = new ArrayList<>();

            for (RoStrParser.RoStrEffect.KeyFrame keyFrame : layer.keyframes()) {
                int texIndex = Math.max(0, Math.min(layer.textures().size() - 1, (int) keyFrame.texId()));
                if (layer.textures().isEmpty()) {
                    warnings.add("Layer " + layerIndex + " has no textures; using fallback texture.");
                }

                ResourceLocation texture = layer.textures().isEmpty()
                        ? new ResourceLocation("minecraft", "textures/block/missingno.png")
                        : toTextureLocation(textureNamespace, textureBasePath, layer.textures().get(texIndex));

                float[] xy = keyFrame.xy();
                float width = xy.length >= 8
                        ? (max(xy[0], xy[2], xy[4], xy[6]) - min(xy[0], xy[2], xy[4], xy[6])) * 0.5f
                        : 0.5f;
                float height = xy.length >= 8
                        ? (max(xy[1], xy[3], xy[5], xy[7]) - min(xy[1], xy[3], xy[5], xy[7])) * 0.5f
                        : 0.5f;
                float[] color = keyFrame.color();

                if (keyFrame.animType() != 0) {
                    warnings.add("Unsupported STR animType=" + keyFrame.animType() + " in " + id
                            + "; using static keyframe.");
                }
                if (keyFrame.materialPreset() != 0) {
                    warnings.add("Unsupported STR material preset=" + keyFrame.materialPreset() + " in " + id + ".");
                }

                keyframes.add(new LayeredEffectDefinition.KeyframeDefinition(
                        (int) keyFrame.frame(),
                        keyFrame.position()[0],
                        keyFrame.position()[1],
                        0.0f,
                        Math.max(0.01f, width),
                        Math.max(0.01f, height),
                        keyFrame.angle(),
                        color.length > 3 ? color[3] : 1.0f,
                        color.length > 0 ? color[0] : 1.0f,
                        color.length > 1 ? color[1] : 1.0f,
                        color.length > 2 ? color[2] : 1.0f,
                        texture));
            }

            keyframes.sort(Comparator.comparingInt(LayeredEffectDefinition.KeyframeDefinition::tick));
            layers.add(new LayeredEffectDefinition.LayerDefinition(
                    "str_layer_" + layerIndex,
                    blendMode,
                    orientation,
                    layerIndex,
                    List.copyOf(keyframes)));
        }

        int durationTicks = durationOverrideTicks > 0 ? durationOverrideTicks : (int) Math.max(effect.maxKey(), 1L);
        LayeredEffectDefinition layered = new LayeredEffectDefinition(id, durationTicks, loop, List.copyOf(layers));

        for (String warning : warnings) {
            LOGGER.warn("STR adapter warning for {}: {}", id, warning);
        }
        return new StrLayeredEffectDefinition(id, layered, List.copyOf(warnings));
    }

    private BlendMode resolveBlendMode(RoStrParser.RoStrEffect.Layer layer, List<String> warnings,
            ResourceLocation id, int layerIndex) {
        if (layer.keyframes().isEmpty()) {
            return BlendMode.TRANSLUCENT;
        }

        RoStrParser.RoStrEffect.KeyFrame first = layer.keyframes().get(0);
        if (first.srcAlpha() == 1 && first.destAlpha() == 1) {
            return BlendMode.ADDITIVE;
        }
        if (first.srcAlpha() == 0 && first.destAlpha() == 0) {
            return BlendMode.TRANSLUCENT;
        }

        warnings.add("Unknown STR blend mode src=" + first.srcAlpha() + " dest=" + first.destAlpha()
                + " on layer " + layerIndex + " of " + id + "; using translucent fallback.");
        return BlendMode.TRANSLUCENT;
    }

    private ResourceLocation toTextureLocation(ResourceLocation namespace, String textureBasePath, String rawTextureName) {
        String normalized = rawTextureName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = fileName.lastIndexOf('.');
        String name = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String path = "textures/effects/" + textureBasePath + "/" + name.toLowerCase(Locale.ROOT) + ".png";
        return ResourceLocation.fromNamespaceAndPath(namespace.getNamespace(), path);
    }

    private float min(float... values) {
        float min = Float.MAX_VALUE;
        for (float value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private float max(float... values) {
        float max = -Float.MAX_VALUE;
        for (float value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
