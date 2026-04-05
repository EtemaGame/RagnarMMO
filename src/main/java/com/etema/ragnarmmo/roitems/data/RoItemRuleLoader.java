package com.etema.ragnarmmo.roitems.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.roitems.network.SyncRoItemRulesPacket;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.system.loot.cards.CardEquipType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads RO item rules from JSON files in data/ragnarmmo/ro_item_rules/.
 * Supports both exact item IDs and tag-based rules (prefixed with #).
 */
public class RoItemRuleLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final RoItemRuleLoader INSTANCE = new RoItemRuleLoader();
    private final RoItemRuleSet ruleSet = new RoItemRuleSet();

    public RoItemRuleLoader() {
        super(GSON, "ro_item_rules");
    }

    public static RoItemRuleLoader getInstance() {
        return INSTANCE;
    }

    public static RoItemRuleSet getRuleSet() {
        return INSTANCE.ruleSet;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        ruleSet.clear();
        RoItemRuleResolver.clearCache();

        resources.forEach((location, jsonElement) -> {
            if (!jsonElement.isJsonObject())
                return;
            JsonObject root = jsonElement.getAsJsonObject();
            parseLegacyItemAndTagRules(location, root);
            parseModTypeRules(location, root);
            parseFallbackRules(location, root);
        });

        RagnarMMO.LOGGER.info("Loaded {} RO item rules ({} by item, {} by tag, {} by mod/type, {} fallbacks)",
                ruleSet.getTotalRuleCount(),
                ruleSet.getItemRuleCount(),
                ruleSet.getTagRuleCount(),
                ruleSet.getModTypeRuleCount(),
                ruleSet.getFallbackRuleCount());

        // Sync to all connected clients after reload
        syncToAllPlayers();
    }

    /**
     * Syncs the current rule set to all connected players.
     * Called after datapack reload.
     */
    private void syncToAllPlayers() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        SyncRoItemRulesPacket packet = new SyncRoItemRulesPacket(
                ruleSet.getItemRules(),
                ruleSet.getTagRules(),
                ruleSet.getModTypeRules(),
                ruleSet.getFallbackRules()
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Network.sendToPlayer(player, packet);
        }
        RagnarMMO.LOGGER.debug("Synced RO item rules to {} players", server.getPlayerList().getPlayerCount());
    }

    /**
     * Syncs rules to a specific player (called on player join).
     */
    public static void syncToPlayer(ServerPlayer player) {
        SyncRoItemRulesPacket packet = new SyncRoItemRulesPacket(
                INSTANCE.ruleSet.getItemRules(),
                INSTANCE.ruleSet.getTagRules(),
                INSTANCE.ruleSet.getModTypeRules(),
                INSTANCE.ruleSet.getFallbackRules()
        );
        Network.sendToPlayer(player, packet);
        RagnarMMO.LOGGER.debug("Synced RO item rules to player {}", player.getName().getString());
    }

    /**
     * Called on client to apply rules received from server.
     */
    public static void applyClientSync(Map<ResourceLocation, RoItemRule> itemRules,
                                       Map<ResourceLocation, RoItemRule> tagRules,
                                       Map<String, Map<CardEquipType, RoItemRule>> modTypeRules,
                                       Map<CardEquipType, RoItemRule> fallbackRules) {
        INSTANCE.ruleSet.clear();
        RoItemRuleResolver.clearCache();

        for (var entry : itemRules.entrySet()) {
            INSTANCE.ruleSet.addItemRule(entry.getKey(), entry.getValue());
        }
        for (var entry : tagRules.entrySet()) {
            INSTANCE.ruleSet.addTagRule(entry.getKey(), entry.getValue());
        }
        for (var modEntry : modTypeRules.entrySet()) {
            for (var typeEntry : modEntry.getValue().entrySet()) {
                INSTANCE.ruleSet.addModTypeRule(modEntry.getKey(), typeEntry.getKey(), typeEntry.getValue());
            }
        }
        for (var entry : fallbackRules.entrySet()) {
            INSTANCE.ruleSet.addFallbackRule(entry.getKey(), entry.getValue());
        }

        RagnarMMO.LOGGER.info("Received {} RO item rules from server ({} by item, {} by tag, {} by mod/type, {} fallbacks)",
                INSTANCE.ruleSet.getTotalRuleCount(),
                INSTANCE.ruleSet.getItemRuleCount(),
                INSTANCE.ruleSet.getTagRuleCount(),
                INSTANCE.ruleSet.getModTypeRuleCount(),
                INSTANCE.ruleSet.getFallbackRuleCount());
    }

    private void parseLegacyItemAndTagRules(ResourceLocation location, JsonObject root) {
        root.entrySet().forEach(entry -> {
            try {
                String key = entry.getKey();
                if ("modEquipmentTypes".equals(key) || "fallbacks".equals(key)) {
                    return;
                }

                if (!entry.getValue().isJsonObject()) {
                    return;
                }

                boolean isTag = key.startsWith("#");
                String targetPath = isTag ? key.substring(1) : key;
                ResourceLocation targetId = new ResourceLocation(targetPath);
                RoItemRule rule = parseRule(entry.getValue().getAsJsonObject(), false);

                if (isTag) {
                    ruleSet.addTagRule(targetId, rule);
                } else {
                    ruleSet.addItemRule(targetId, rule);
                }
            } catch (Exception e) {
                RagnarMMO.LOGGER.warn("Failed to parse RO item rule '{}' in {}: {}",
                        entry.getKey(), location, e.getMessage());
            }
        });
    }

    private void parseModTypeRules(ResourceLocation location, JsonObject root) {
        if (!root.has("modEquipmentTypes") || !root.get("modEquipmentTypes").isJsonObject()) {
            return;
        }

        JsonObject modRules = root.getAsJsonObject("modEquipmentTypes");
        modRules.entrySet().forEach(modEntry -> {
            String modId = modEntry.getKey().trim().toLowerCase(Locale.ROOT);
            if (!modEntry.getValue().isJsonObject()) {
                return;
            }

            JsonObject typeRules = modEntry.getValue().getAsJsonObject();
            typeRules.entrySet().forEach(typeEntry -> {
                try {
                    CardEquipType equipType = CardEquipType.fromString(typeEntry.getKey());
                    if (equipType == CardEquipType.ANY || !typeEntry.getValue().isJsonObject()) {
                        return;
                    }
                    RoItemRule rule = parseRule(typeEntry.getValue().getAsJsonObject(), true);
                    ruleSet.addModTypeRule(modId, equipType, rule);
                } catch (Exception e) {
                    RagnarMMO.LOGGER.warn("Failed to parse RO mod/type rule '{}:{}' in {}: {}",
                            modId, typeEntry.getKey(), location, e.getMessage());
                }
            });
        });
    }

    private void parseFallbackRules(ResourceLocation location, JsonObject root) {
        if (!root.has("fallbacks") || !root.get("fallbacks").isJsonObject()) {
            return;
        }

        JsonObject fallbacks = root.getAsJsonObject("fallbacks");
        fallbacks.entrySet().forEach(entry -> {
            try {
                CardEquipType equipType = CardEquipType.fromString(entry.getKey());
                if (equipType == CardEquipType.ANY || !entry.getValue().isJsonObject()) {
                    return;
                }
                RoItemRule rule = parseRule(entry.getValue().getAsJsonObject(), true);
                ruleSet.addFallbackRule(equipType, rule);
            } catch (Exception e) {
                RagnarMMO.LOGGER.warn("Failed to parse RO fallback rule '{}' in {}: {}",
                        entry.getKey(), location, e.getMessage());
            }
        });
    }

    /**
     * Parse a single rule from JSON.
     *
     * Expected format:
     * {
     * "displayName": "Blade [3]",
     * "weaponLevel": 3,
     * "weight": 90.0,
     * "attributeBonuses": { "str": 5, "agi": 3 },
     * "requiredBaseLevel": 25,
     * "allowedJobs": ["SWORDSMAN", "THIEF"],
     * "cardSlots": 3
     * }
     */
    private RoItemRule parseRule(JsonObject json, boolean defaultShowTooltip) {
        String displayName = getStringOrNull(json, "displayName");
        int requiredBaseLevel = getIntOrDefault(json, "requiredBaseLevel", 0);
        int cardSlots = getIntOrDefault(json, "cardSlots", 0);
        boolean showTooltip = getBooleanOrDefault(json, "showTooltip", defaultShowTooltip);

        // Parse attribute bonuses
        Map<StatKeys, Integer> attributeBonuses = new EnumMap<>(StatKeys.class);
        if (json.has("attributeBonuses") && json.get("attributeBonuses").isJsonObject()) {
            JsonObject bonuses = json.getAsJsonObject("attributeBonuses");
            for (StatKeys stat : StatKeys.values()) {
                String statName = stat.name().toLowerCase(Locale.ROOT);
                if (bonuses.has(statName)) {
                    attributeBonuses.put(stat, bonuses.get(statName).getAsInt());
                }
            }
        }

        // Parse allowed jobs
        Set<JobType> allowedJobs = EnumSet.noneOf(JobType.class);
        if (json.has("allowedJobs") && json.get("allowedJobs").isJsonArray()) {
            json.getAsJsonArray("allowedJobs").forEach(element -> {
                String jobName = element.getAsString().toUpperCase(Locale.ROOT);
                try {
                    allowedJobs.add(JobType.valueOf(jobName));
                } catch (IllegalArgumentException e) {
                    RagnarMMO.LOGGER.warn("Unknown job type in RO item rule: {}", jobName);
                }
            });
        }

        return new RoItemRule(displayName, attributeBonuses, requiredBaseLevel, allowedJobs, cardSlots, showTooltip);

    }

    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsString();
        }
        return null;
    }

    private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsInt();
        }
        return defaultValue;
    }

    private boolean getBooleanOrDefault(JsonObject json, String key, boolean defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * Event handler class for registering the reload listener.
     */
    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            // Sync to specific player on join, or all players on /reload
            ServerPlayer player = event.getPlayer();
            if (player != null) {
                // Single player joining
                syncToPlayer(player);
            }
            // Note: For /reload, the apply() method already calls syncToAllPlayers()
        }
    }
}
