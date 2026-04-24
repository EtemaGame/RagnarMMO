package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and manages life skill point sources from JSON files.
 * Location: data/ragnarmmo/life_points_sources/*.json
 *
 * JSON Format:
 * {
 * "minecraft:coal_ore": { "skill": "mining", "mode": "per_10", "points": 2 },
 * "minecraft:diamond_ore": { "skill": "mining", "mode": "per_block", "points":
 * 10 },
 * "#minecraft:logs": { "skill": "woodcutting", "mode": "per_10", "points": 1 }
 * }
 *
 * Modes:
 * - "per_block": Award points immediately per block
 * - "per_10": Accumulate counter, award points every 10 blocks (or custom
 * threshold)
 */
public class LifePointsSourceConfig extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifePointsSourceConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "life_points_sources";

    private static LifePointsSourceConfig INSTANCE;

    // Block ID -> PointSource
    private final Map<ResourceLocation, PointSource> blockSources = new HashMap<>();
    // Tag -> PointSource (for tag-based matching like #minecraft:logs)
    private final Map<TagKey<Block>, PointSource> tagSources = new HashMap<>();

    public LifePointsSourceConfig() {
        super(GSON, DIRECTORY);
        INSTANCE = this;
    }

    public static LifePointsSourceConfig getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        blockSources.clear();
        tagSources.clear();

        int blockCount = 0;
        int tagCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonObject root = entry.getValue().getAsJsonObject();

                for (Map.Entry<String, JsonElement> sourceEntry : root.entrySet()) {
                    String key = sourceEntry.getKey();
                    if (!sourceEntry.getValue().isJsonObject()) {
                        // Skip metadata entries like "_comment", "schemaVersion", etc.
                        continue;
                    }
                    JsonObject sourceData = sourceEntry.getValue().getAsJsonObject();

                    PointSource source = parseSource(sourceData);
                    if (source == null)
                        continue;

                    if (key.startsWith("#")) {
                        // Tag-based source
                        String tagName = key.substring(1);
                        ResourceLocation tagId = new ResourceLocation(tagName);
                        TagKey<Block> tag = BlockTags.create(tagId);
                        tagSources.put(tag, source);
                        tagCount++;
                    } else {
                        // Direct block ID
                        ResourceLocation blockId = new ResourceLocation(key);
                        blockSources.put(blockId, source);
                        blockCount++;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error loading life points source file: {}", entry.getKey(), e);
            }
        }

        LOGGER.info("Loaded {} block sources and {} tag sources for life skills",
                blockCount, tagCount);
    }

    private PointSource parseSource(JsonObject data) {
        if (!data.has("skill") || !data.has("points")) {
            return null;
        }

        String skillId = data.get("skill").getAsString();
        LifeSkillType skill = LifeSkillType.fromId(skillId);
        if (skill == null) {
            LOGGER.warn("Invalid life skill: {}", skillId);
            return null;
        }

        int points = data.get("points").getAsInt();
        String mode = data.has("mode") ? data.get("mode").getAsString() : "per_block";
        int threshold = data.has("threshold") ? data.get("threshold").getAsInt() : 10;

        return new PointSource(skill, points, mode.equals("per_10") || mode.equals("per_threshold"), threshold);
    }

    /**
     * Get point source for a direct ResourceLocation (useful for items, entities,
     * etc).
     */
    public PointSource getSource(ResourceLocation id) {
        return blockSources.get(id); // Uses the same map since it's just ID -> Source
    }

    /**
     * Get point source for a block state.
     */
    public PointSource getSource(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        // Check direct block ID first
        if (blockId != null && blockSources.containsKey(blockId)) {
            return blockSources.get(blockId);
        }

        // Check tags
        for (Map.Entry<TagKey<Block>, PointSource> entry : tagSources.entrySet()) {
            if (state.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Check if a block has a life skill point source.
     */
    public boolean hasSource(BlockState state) {
        return getSource(state) != null;
    }

    /**
     * Point source configuration for a block/tag.
     */
    public static class PointSource {
        private final LifeSkillType skill;
        private final int points;
        private final boolean useThreshold; // true = accumulate, false = per block
        private final int threshold; // blocks needed to earn points (default 10)

        public PointSource(LifeSkillType skill, int points, boolean useThreshold, int threshold) {
            this.skill = skill;
            this.points = points;
            this.useThreshold = useThreshold;
            this.threshold = threshold;
        }

        public LifeSkillType getSkill() {
            return skill;
        }

        public int getPoints() {
            return points;
        }

        public boolean usesThreshold() {
            return useThreshold;
        }

        public int getThreshold() {
            return threshold;
        }
    }

    /**
     * Event handler to register this as a reload listener.
     */
    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
    public static class Events {
        private static final LifePointsSourceConfig CONFIG = new LifePointsSourceConfig();

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(CONFIG);
        }
    }
}
