package com.etema.ragnarmmo.skills.runtime;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Loads XP sources from JSON.
 * Data-driven approach for Skills.
 */
public class SourceConfig extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final SourceConfig INSTANCE = new SourceConfig();

    // Map<ID, Map<SkillID, XP>>
    private final Map<ResourceLocation, Map<String, Integer>> xpById = new HashMap<>();
    // Map<TagID, Map<SkillID, XP>>
    private final Map<ResourceLocation, Map<String, Integer>> xpByTag = new HashMap<>();

    public SourceConfig() {
        super(GSON, "xp_sources");
    }

    public static SourceConfig getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn,
            ProfilerFiller profilerIn) {
        xpById.clear();
        xpByTag.clear();

        objectIn.forEach((location, jsonElement) -> {
            if (!jsonElement.isJsonObject())
                return;
            JsonObject json = jsonElement.getAsJsonObject();

            // Format: "minecraft:stone": { "mining": 10 }
            // Format: "#minecraft:logs": { "woodcutting": 20 }
            json.entrySet().forEach(entry -> {
                String key = entry.getKey();
                boolean isTag = key.startsWith("#");
                ResourceLocation targetId = new ResourceLocation(isTag ? key.substring(1) : key);
                JsonObject skills = entry.getValue().getAsJsonObject();

                Map<String, Integer> skillXp = new HashMap<>();
                skills.entrySet().forEach(skillEntry -> {
                    skillXp.put(skillEntry.getKey(), skillEntry.getValue().getAsInt());
                });

                if (isTag) {
                    xpByTag.put(targetId, skillXp);
                } else {
                    xpById.put(targetId, skillXp);
                }
            });
        });
    }

    public int getXp(ResourceLocation targetId, ResourceLocation skillId) {
        if (xpById.containsKey(targetId)) {
            var map = xpById.get(targetId);
            if (map.containsKey(skillId.toString()))
                return map.get(skillId.toString());
            if (map.containsKey(skillId.getPath()))
                return map.get(skillId.getPath());
        }
        return 0;
    }

    // Check ID first, then Tags implies logic needs access to BlockState/Entity to
    // check tags.
    // Since we only have ResourceLocation here in the simple signature, we might
    // need a better method.
    // For the prototype path, if we pass a Block ID, we can't easily check Tags without the Block
    // object or World.
    // BUT, we can use ForgeRegistries to look up the Block/Entity from ID and check
    // tags.

    public int getXp(net.minecraft.world.level.block.state.BlockState state, ResourceLocation skillId) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        // 1. Check ID override
        if (xpById.containsKey(id)) {
            var map = xpById.get(id);
            if (map.containsKey(skillId.toString()))
                return map.get(skillId.toString());
            if (map.containsKey(skillId.getPath()))
                return map.get(skillId.getPath());
        }

        // 2. Check Tags
        for (var entry : xpByTag.entrySet()) {
            net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tagKey = net.minecraft.tags.TagKey
                    .create(net.minecraft.core.registries.Registries.BLOCK, entry.getKey());
            if (state.is(tagKey)) {
                var map = entry.getValue();
                if (map.containsKey(skillId.toString()))
                    return map.get(skillId.toString());
                if (map.containsKey(skillId.getPath()))
                    return map.get(skillId.getPath());
            }
        }

        return 0;
    }

    public int getXp(net.minecraft.world.entity.LivingEntity entity, ResourceLocation skillId) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        // 1. Check ID override
        if (xpById.containsKey(id)) {
            var map = xpById.get(id);
            if (map.containsKey(skillId.toString()))
                return map.get(skillId.toString());
            if (map.containsKey(skillId.getPath()))
                return map.get(skillId.getPath());
        }

        // 2. Check Tags
        for (var entry : xpByTag.entrySet()) {
            net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tagKey = net.minecraft.tags.TagKey
                    .create(net.minecraft.core.registries.Registries.ENTITY_TYPE, entry.getKey());
            if (entity.getType().is(tagKey)) {
                var map = entry.getValue();
                if (map.containsKey(skillId.toString()))
                    return map.get(skillId.toString());
                if (map.containsKey(skillId.getPath()))
                    return map.get(skillId.getPath());
            }
        }

        return 0;
    }

    @Mod.EventBusSubscriber(modid = "ragnarmmo")
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }
    }
}
