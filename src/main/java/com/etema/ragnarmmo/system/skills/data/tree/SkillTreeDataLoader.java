package com.etema.ragnarmmo.system.skills.data.tree;

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
 * Loads skill tree layouts from data/{namespace}/skill_trees/*.json
 *
 * Example file: data/ragnarmmo/skill_trees/mage_1.json
 */
public class SkillTreeDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "skill_trees";

    public SkillTreeDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        LOGGER.info("Loading skill tree layouts...");

        SkillTreeRegistry.clear();
        int loaded = 0;
        int failed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try {
                if (!entry.getValue().isJsonObject()) {
                    LOGGER.warn("Skill tree {} is not a JSON object, skipping", fileId);
                    failed++;
                    continue;
                }

                JsonObject json = entry.getValue().getAsJsonObject();
                SkillTreeDefinition tree = parseSkillTree(fileId, json);

                if (tree != null) {
                    SkillTreeRegistry.register(tree);
                    loaded++;
                } else {
                    failed++;
                }

            } catch (Exception e) {
                LOGGER.error("Failed to load skill tree {}", fileId, e);
                failed++;
            }
        }

        SkillTreeRegistry.freeze();
        LOGGER.info("Loaded {} skill tree layouts ({} failed)", loaded, failed);
    }

    private SkillTreeDefinition parseSkillTree(ResourceLocation fileId, JsonObject json) {
        try {
            // Required fields
            String job = json.has("job") ? json.get("job").getAsString() : "NOVICE";
            int tier = json.has("tier") ? json.get("tier").getAsInt() : 1;

            // Grid dimensions
            int gridWidth = 10;
            int gridHeight = 7;
            if (json.has("grid")) {
                JsonObject grid = json.getAsJsonObject("grid");
                gridWidth = grid.has("width") ? grid.get("width").getAsInt() : 10;
                gridHeight = grid.has("height") ? grid.get("height").getAsInt() : 7;
            }

            // Build the tree
            SkillTreeDefinition.Builder builder = SkillTreeDefinition.builder(fileId)
                    .job(job)
                    .tier(tier)
                    .gridSize(gridWidth, gridHeight);

            // Inheritance
            if (json.has("inherit_from")) {
                if (json.get("inherit_from").isJsonArray()) {
                    json.getAsJsonArray("inherit_from").forEach(el -> {
                        String parentId = el.getAsString();
                        builder.inheritFrom(parseResourceLocation(parentId));
                    });
                } else {
                    String parentId = json.get("inherit_from").getAsString();
                    builder.inheritFrom(parseResourceLocation(parentId));
                }
            }

            // Skills array
            if (json.has("skills")) {
                json.getAsJsonArray("skills").forEach(el -> {
                    if (el.isJsonObject()) {
                        JsonObject skillNode = el.getAsJsonObject();
                        String skillId = skillNode.get("id").getAsString();
                        int x = skillNode.get("x").getAsInt();
                        int y = skillNode.get("y").getAsInt();

                        builder.addSkill(parseResourceLocation(skillId), x, y);
                    }
                });
            }

            return builder.build();

        } catch (Exception e) {
            LOGGER.error("Failed to parse skill tree {}", fileId, e);
            return null;
        }
    }

    private ResourceLocation parseResourceLocation(String id) {
        if (id.contains(":")) {
            return ResourceLocation.parse(id);
        } else {
            // Default to ragnarmmo namespace
            return ResourceLocation.fromNamespaceAndPath("ragnarmmo", id);
        }
    }

    /**
     * Event handler class for registering the reload listener.
     */
    @Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID)
    public static class Events {
        private static final SkillTreeDataLoader INSTANCE = new SkillTreeDataLoader();

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
            LOGGER.info("Registered skill tree data loader");
        }
    }
}
