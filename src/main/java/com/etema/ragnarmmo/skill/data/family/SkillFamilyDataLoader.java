package com.etema.ragnarmmo.skill.data.family;

import com.etema.ragnarmmo.system.stats.RagnarStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Data loader for skill families.
 * Loads from data/&#42;/skill_families/&#42;.json
 */
public class SkillFamilyDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillFamilyDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "skill_families";

    public static final SkillFamilyDataLoader INSTANCE = new SkillFamilyDataLoader();

    private SkillFamilyDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager,
            ProfilerFiller pProfiler) {
        LOGGER.info("Loading skill families from data packs...");
        SkillFamilyRegistry.clear();

        int loaded = 0;
        int failed = 0;

        for (var entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                SkillFamily family = parseFamily(id, json);
                SkillFamilyRegistry.register(family);
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Failed to load skill family {}: {}", id, e.getMessage(), e);
                failed++;
            }
        }

        SkillFamilyRegistry.freeze();
        LOGGER.info("Loaded {} skill families ({} failed)", loaded, failed);
    }

    private SkillFamily parseFamily(ResourceLocation id, JsonObject json) {
        // Full ID with namespace
        ResourceLocation fullId = new ResourceLocation(json.has("id") ? parseId(json.get("id").getAsString()).getNamespace() : id.getNamespace(), json.has("id") ? parseId(json.get("id").getAsString()).getPath() : id.getPath());

        SkillFamily.Builder builder = SkillFamily.builder(fullId);

        // Parse defaults object
        if (json.has("defaults") && json.get("defaults").isJsonObject()) {
            JsonObject defaults = json.getAsJsonObject("defaults");

            // Iterate through all defaults and add them
            for (var entry : defaults.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                Object parsedValue = parseValue(value);
                if (parsedValue != null) {
                    builder.setDefault(key, parsedValue);
                }
            }
        }

        return builder.build();
    }

    /**
     * Parse a JsonElement into a Java object (String, Number, Boolean, Map, List).
     */
    private Object parseValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                double d = primitive.getAsDouble();
                // Return int if it's a whole number, otherwise double
                if (d == Math.floor(d)) {
                    return primitive.getAsInt();
                }
                return d;
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
        } else if (element.isJsonObject()) {
            // Return as Map<String, Object>
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            JsonObject obj = element.getAsJsonObject();
            for (var entry : obj.entrySet()) {
                map.put(entry.getKey(), parseValue(entry.getValue()));
            }
            return map;
        } else if (element.isJsonArray()) {
            // Return as List<Object>
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(parseValue(item));
            }
            return list;
        }
        return null;
    }

    private ResourceLocation parseId(String id) {
        if (id.contains(":")) {
            String[] parts = id.split(":", 2);
            return new ResourceLocation(parts[0], parts[1]);
        }
        return new ResourceLocation("ragnarmmo", id);
    }

    /**
     * Register this data loader to the reload listener event.
     */
    @Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID)
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
            LOGGER.debug("Registered SkillFamilyDataLoader");
        }
    }
}
