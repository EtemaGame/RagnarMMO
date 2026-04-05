package com.etema.ragnarmmo.system.loot.cards;

import com.etema.ragnarmmo.RagnarMMO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads card definitions from {@code data/ragnarmmo/cards/*.json}.
 */
public class CardDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final CardDataLoader INSTANCE = new CardDataLoader();

    public CardDataLoader() {
        super(GSON, "cards");
    }

    public static CardDataLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries,
            ResourceManager manager, ProfilerFiller profiler) {
        CardRegistry registry = CardRegistry.getInstance();
        registry.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();

                String id = json.get("id").getAsString();
                String displayName = json.get("displayName").getAsString();
                String mobId = json.get("mobId").getAsString();
                double dropRate = json.get("dropRate").getAsDouble();
                CardEquipType equipType = json.has("equipType")
                        ? CardEquipType.fromString(json.get("equipType").getAsString())
                        : CardEquipType.ANY;
                String translationKey = json.has("translationKey") ? json.get("translationKey").getAsString()
                        : "card.ragnarmmo." + id + ".desc";

                // Parse modifiers
                Map<String, Double> modifiers = new HashMap<>();
                if (json.has("modifiers")) {
                    JsonObject mods = json.getAsJsonObject("modifiers");
                    for (Map.Entry<String, JsonElement> mod : mods.entrySet()) {
                        modifiers.put(mod.getKey(), mod.getValue().getAsDouble());
                    }
                }

                int modelId = json.has("modelId") ? json.get("modelId").getAsInt() : 0;

                CardDefinition def = new CardDefinition(id, displayName, mobId, modifiers, dropRate, equipType,
                        translationKey, modelId);
                registry.register(def);
                count++;

            } catch (Exception e) {
                LOGGER.error("Failed to load card definition from {}: {}", entry.getKey(), e.getMessage());
            }
        }

        LOGGER.info("RagnarMMO: Loaded {} card definitions", count);
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }
    }
}
