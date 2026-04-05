package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.RagnarMMO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads block-layer projectile visuals from assets so resourcepacks can override them.
 */
public final class SkillMaterialVisualDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillMaterialVisualDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final SkillMaterialVisualDataLoader INSTANCE = new SkillMaterialVisualDataLoader();

    private SkillMaterialVisualDataLoader() {
        super(GSON, "skill_material_visuals");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        profiler.push("skill_material_visuals");
        SkillMaterialVisualsRegistry.clear();

        int loaded = 0;
        int failed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                failed++;
                continue;
            }

            try {
                parse(entry.getKey(), entry.getValue().getAsJsonObject());
                loaded++;
            } catch (Exception e) {
                failed++;
                LOGGER.warn("Failed to load skill material visual {}: {}", entry.getKey(), e.getMessage());
            }
        }

        LOGGER.info("Loaded {} block-based skill visual declarations ({} failed)", loaded, failed);
        profiler.pop();
    }

    private void parse(ResourceLocation fileId, JsonObject json) {
        ResourceLocation skillId = parseResourceLocation(getStringOrDefault(json, "skill_id", null));
        if (skillId == null) {
            skillId = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath());
        }

        List<BlockLayerVisual> layers = new ArrayList<>();
        if (json.has("layers") && json.get("layers").isJsonArray()) {
            JsonArray jsonLayers = json.getAsJsonArray("layers");
            for (JsonElement layerElement : jsonLayers) {
                if (!layerElement.isJsonObject()) {
                    continue;
                }

                JsonObject layer = layerElement.getAsJsonObject();
                ResourceLocation blockId = parseResourceLocation(getStringOrDefault(layer, "block", null));
                if (blockId == null) {
                    continue;
                }

                float[] scale = parseTriplet(layer, "scale", 1.0f, 1.0f, 1.0f);
                layers.add(new BlockLayerVisual(
                        blockId,
                        scale[0],
                        scale[1],
                        scale[2],
                        (float) getDoubleOrDefault(layer, "rotation_x", 0.0),
                        (float) getDoubleOrDefault(layer, "rotation_y", 0.0),
                        (float) getDoubleOrDefault(layer, "rotation_z", 0.0)));
            }
        }

        if (!layers.isEmpty()) {
            SkillMaterialVisualsRegistry.register(skillId, layers);
        }
    }

    private float[] parseTriplet(JsonObject json, String key, float fallbackX, float fallbackY, float fallbackZ) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return new float[] { fallbackX, fallbackY, fallbackZ };
        }

        JsonArray array = json.getAsJsonArray(key);
        if (array.size() < 3) {
            return new float[] { fallbackX, fallbackY, fallbackZ };
        }

        return new float[] {
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        };
    }

    private String getStringOrDefault(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private double getDoubleOrDefault(JsonObject json, String key, double fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : fallback;
    }

    private ResourceLocation parseResourceLocation(String value) {
        return value != null && !value.isBlank() ? ResourceLocation.tryParse(value) : null;
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Events {
        @SubscribeEvent
        public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
