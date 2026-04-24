package com.etema.ragnarmmo.skill.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skill.api.SkillCategory;
import com.etema.ragnarmmo.skill.api.SkillTier;
import com.etema.ragnarmmo.skill.api.SkillUsageType;
import com.etema.ragnarmmo.skill.api.SkillEffectFactory;
import com.etema.ragnarmmo.skill.net.SyncSkillDefinitionsPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Loads skill definitions from JSON files in data/{namespace}/skills/.
 * Implements ResourceManagerReloadListener for /reload support.
 *
 * Skill definitions are JSON-authored only.
 */
public class SkillDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<ResourceLocation> CANONICAL_FIRST_JOB_SKILL_IDS = Set.of(
            skillId("owls_eye"),
            skillId("vultures_eye"),
            skillId("improve_concentration"),
            skillId("double_strafe"),
            skillId("arrow_shower"),

            skillId("sword_mastery"),
            skillId("increase_hp_recovery"),
            skillId("bash"),
            skillId("provoke"),
            skillId("two_hand_mastery"),
            skillId("magnum_break"),
            skillId("endure"),

            skillId("increase_sp_recovery"),
            skillId("sight"),
            skillId("napalm_beat"),
            skillId("soul_strike"),
            skillId("safety_wall"),
            skillId("cold_bolt"),
            skillId("frost_diver"),
            skillId("stone_curse"),
            skillId("fire_bolt"),
            skillId("fire_ball"),
            skillId("fire_wall"),
            skillId("lightning_bolt"),
            skillId("thunder_storm"),

            skillId("divine_protection"),
            skillId("demon_bane"),
            skillId("angelus"),
            skillId("blessing"),
            skillId("heal"),
            skillId("increase_agi"),
            skillId("decrease_agi"),
            skillId("cure"),
            skillId("ruwach"),
            skillId("teleportation"),
            skillId("warp_portal"),
            skillId("pneuma"),
            skillId("aqua_benedicta"),
            skillId("holy_light"),
            skillId("signum_crucis"),

            skillId("double_attack"),
            skillId("improve_dodge"),
            skillId("steal"),
            skillId("hiding"),
            skillId("envenom"),
            skillId("detoxify"),

            skillId("enlarge_weight_limit"),
            skillId("discount"),
            skillId("overcharge"),
            skillId("pushcart"),
            skillId("vending"),
            skillId("buying_store"),
            skillId("identify"),
            skillId("mammonite"));

    public static final SkillDataLoader INSTANCE = new SkillDataLoader();

    private SkillDataLoader() {
        super(GSON, "skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        profiler.push("skill_loader");

        SkillRegistry.clear();

        // Phase 1: Load from JSON files
        int jsonCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement element = entry.getValue();

            if (!element.isJsonObject()) {
                LOGGER.warn("Skill file {} is not a JSON object, skipping", fileId);
                errorCount++;
                continue;
            }

            try {
                SkillDefinition def = parseSkillJson(fileId, element.getAsJsonObject());
                if (def != null) {
                    SkillRegistry.register(def);
                    jsonCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse skill file {}: {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        LOGGER.info("Loaded {} skills from JSON ({} errors)", jsonCount, errorCount);
        boolean enforceCanonicalSkills = !resources.isEmpty();
        if (!enforceCanonicalSkills) {
            LOGGER.warn("No skill JSON resources were found in this reload context; skipping canonical validation");
        } else {
            validateCanonicalSkillsPresent();
        }

        // Phase 2: Auto-register effects from effect_class
        int effectCount = 0;
        int effectErrors = 0;
        for (SkillDefinition def : SkillRegistry.getAll()) {
            String effectIdentifier = def.getEffectClass();
            if (effectIdentifier == null || effectIdentifier.isEmpty()) {
                continue;
            }

            var effectOpt = SkillEffectFactory.create(effectIdentifier, def.getId());
            if (effectOpt.isPresent()) {
                SkillRegistry.registerEffect(def.getId(), effectOpt.get());
                effectCount++;
            } else {
                LOGGER.warn("Effect not found or failed to instantiate for skill {}: {}", def.getId(), effectIdentifier);
                effectErrors++;
            }
        }

        if (effectCount > 0) {
            LOGGER.info("Registered {} skill effects ({} errors)", effectCount, effectErrors);
        }

        SkillRegistry.freeze();
        LOGGER.info("SkillRegistry ready: {} total skills", SkillRegistry.size());

        profiler.pop();
        
        // Sync to all connected players after reload if on server
        syncToAll();
    }

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, path);
    }

    private static boolean isCanonicalFirstJobSkill(ResourceLocation id) {
        return CANONICAL_FIRST_JOB_SKILL_IDS.contains(id);
    }

    private static void validateCanonicalSkillsPresent() {
        List<ResourceLocation> missing = CANONICAL_FIRST_JOB_SKILL_IDS.stream()
                .filter(id -> !SkillRegistry.contains(id))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        if (!missing.isEmpty()) {
            LOGGER.error("Missing canonical first-job skill JSON definitions: {}", missing);
            throw new IllegalStateException("Missing canonical first-job skill JSON definitions: " + missing);
        }
    }

    public void syncToAll() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        SyncSkillDefinitionsPacket packet = new SyncSkillDefinitionsPacket(SkillRegistry.getAll());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, packet);
        }
        RagnarMMO.LOGGER.debug("Synced skill definitions to {} players", server.getPlayerList().getPlayerCount());
    }

    public void syncToPlayer(ServerPlayer player) {
        SyncSkillDefinitionsPacket packet = new SyncSkillDefinitionsPacket(SkillRegistry.getAll());
        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, packet);
        RagnarMMO.LOGGER.debug("Synced skill definitions to player {}", player.getName().getString());
    }

    /**
     * Parse a skill definition from JSON.
     */
    private SkillDefinition parseSkillJson(ResourceLocation fileId, JsonObject json) {
        // Check if this skill inherits from a family
        if (json.has("family")) {
            json = applyFamilyDefaults(json);
        }

        // Required fields
        String idStr = getStringOrDefault(json, "id", fileId.getNamespace() + ":" + fileId.getPath());
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) {
            LOGGER.warn("Invalid skill ID in {}: {}", fileId, idStr);
            return null;
        }

        String displayName = getStringOrDefault(json, "display_name", id.getPath());

        // Classification
        SkillCategory category = parseCategory(getStringOrDefault(json, "category", "CLASS_PASSIVE"));
        SkillTier tier = parseTier(getStringOrDefault(json, "tier", "FIRST"));
        SkillUsageType usage = parseUsage(getStringOrDefault(json, "usage", "PASSIVE"));
        String scalingStat = getStringOrDefault(json, "scaling_stat", "STR");
        double xpMultiplier = getDoubleOrDefault(json, "xp_multiplier", 1.0);

        // Costs
        int baseCost = 0;
        int costPerLevel = 2;
        if (json.has("costs") && json.get("costs").isJsonObject()) {
            JsonObject costs = json.getAsJsonObject("costs");
            baseCost = getIntOrDefault(costs, "base_cost", 0);
            costPerLevel = getIntOrDefault(costs, "cost_per_level", 2);
        } else {
            baseCost = getIntOrDefault(json, "base_cost", 0);
            costPerLevel = getIntOrDefault(json, "cost_per_level", 2);
        }

        // Timing
        int cooldownTicks = getIntOrDefault(json, "cooldown_ticks", 20);
        int castDelayTicks = getIntOrDefault(json, "cast_delay_ticks", 0);
        int castTimeTicks = getIntOrDefault(json, "cast_time_ticks", 0);
        boolean interruptible = getBooleanOrDefault(json, "interruptible", true);

        // Progression
        int maxLevel = 10;
        int upgradeCost = 1;
        boolean canGainXp = false;
        boolean canUpgradeWithPoints = true;

        if (json.has("progression") && json.get("progression").isJsonObject()) {
            JsonObject prog = json.getAsJsonObject("progression");
            maxLevel = getIntOrDefault(prog, "max_level", 10);
            upgradeCost = getIntOrDefault(prog, "upgrade_cost", 1);
            canGainXp = getBooleanOrDefault(prog, "can_gain_xp", false);
            canUpgradeWithPoints = getBooleanOrDefault(prog, "can_upgrade_with_points", true);
        } else {
            maxLevel = getIntOrDefault(json, "max_level", 10);
        }

        // Requirements
        Map<ResourceLocation, Integer> requirements = null;
        if (json.has("requirements") && json.get("requirements").isJsonObject()) {
            requirements = new HashMap<>();
            JsonObject reqs = json.getAsJsonObject("requirements");
            for (Map.Entry<String, JsonElement> req : reqs.entrySet()) {
                ResourceLocation reqId = ResourceLocation.tryParse(req.getKey());
                if (reqId != null && req.getValue().isJsonPrimitive()) {
                    requirements.put(reqId, req.getValue().getAsInt());
                }
            }
        }

        // Jobs
        Set<String> allowedJobs = null;
        if (json.has("jobs") && json.get("jobs").isJsonArray()) {
            allowedJobs = new HashSet<>();
            for (JsonElement jobEl : json.getAsJsonArray("jobs")) {
                if (jobEl.isJsonPrimitive()) {
                    allowedJobs.add(jobEl.getAsString().toUpperCase(Locale.ROOT));
                }
            }
        }

        // UI
        String textureName = id.getPath();
        int gridX = 0;
        int gridY = 0;
        ResourceLocation icon = null;

        if (json.has("ui") && json.get("ui").isJsonObject()) {
            JsonObject ui = json.getAsJsonObject("ui");
            textureName = getStringOrDefault(ui, "texture", id.getPath());
            gridX = getIntOrDefault(ui, "grid_x", 0);
            gridY = getIntOrDefault(ui, "grid_y", 0);
            String iconStr = getStringOrDefault(ui, "icon", null);
            if (iconStr != null) {
                icon = ResourceLocation.tryParse(iconStr);
            }
        } else {
            textureName = getStringOrDefault(json, "texture", id.getPath());
            gridX = getIntOrDefault(json, "grid_x", 0);
            gridY = getIntOrDefault(json, "grid_y", 0);
        }

        // Effect
        String effectClass = getStringOrDefault(json, "effect_class", null);

        SkillReference reference = parseReference(json);
        Map<Integer, SkillLevelData> levelData = parseLevelData(json);

        return SkillDefinition.builder(id)
                .displayName(displayName)
                .category(category)
                .tier(tier)
                .usageType(usage)
                .scalingStat(scalingStat)
                .xpMultiplier(xpMultiplier)
                .baseCost(baseCost)
                .costPerLevel(costPerLevel)
                .cooldownTicks(cooldownTicks)
                .castDelayTicks(castDelayTicks)
                .castTimeTicks(castTimeTicks)
                .interruptible(interruptible)
                .maxLevel(maxLevel)
                .upgradeCost(upgradeCost)
                .canGainXp(canGainXp)
                .canUpgradeWithPoints(canUpgradeWithPoints)
                .requirements(requirements)
                .allowedJobs(allowedJobs)
                .textureName(textureName)
                .gridX(gridX)
                .gridY(gridY)
                .icon(icon)
                .effectClass(effectClass)
                .reference(reference)
                .levelData(levelData)
                .build();
    }

    private SkillReference parseReference(JsonObject json) {
        if (!json.has("reference") || !json.get("reference").isJsonObject()) {
            return null;
        }

        JsonObject ref = json.getAsJsonObject("reference");
        String sourceName = getStringOrDefault(ref, "source_name", "");
        String sourceUrl = getStringOrDefault(ref, "source_url", "");
        String notes = getStringOrDefault(ref, "notes", "");
        return new SkillReference(sourceName, sourceUrl, notes);
    }

    private Map<Integer, SkillLevelData> parseLevelData(JsonObject json) {
        if (!json.has("level_data") || !json.get("level_data").isJsonObject()) {
            return Map.of();
        }

        Map<Integer, SkillLevelData> parsed = new HashMap<>();
        JsonObject levels = json.getAsJsonObject("level_data");

        for (Map.Entry<String, JsonElement> entry : levels.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException ignored) {
                continue;
            }

            JsonObject levelJson = entry.getValue().getAsJsonObject();
            Map<String, Double> numericValues = new HashMap<>();
            Map<String, String> stringValues = new HashMap<>();
            Map<String, Boolean> booleanValues = new HashMap<>();

            for (Map.Entry<String, JsonElement> valueEntry : levelJson.entrySet()) {
                JsonElement value = valueEntry.getValue();
                if (!value.isJsonPrimitive()) {
                    continue;
                }

                if (value.getAsJsonPrimitive().isNumber()) {
                    numericValues.put(valueEntry.getKey(), value.getAsDouble());
                } else if (value.getAsJsonPrimitive().isBoolean()) {
                    booleanValues.put(valueEntry.getKey(), value.getAsBoolean());
                } else if (value.getAsJsonPrimitive().isString()) {
                    stringValues.put(valueEntry.getKey(), value.getAsString());
                }
            }

            parsed.put(level, new SkillLevelData(numericValues, stringValues, booleanValues));
        }

        return parsed;
    }

    /**
     * Apply defaults from a skill family to a skill JSON.
     * Skill-specific values override family defaults.
     */
    private JsonObject applyFamilyDefaults(JsonObject skillJson) {
        String familyId = getStringOrDefault(skillJson, "family", null);
        if (familyId == null) {
            return skillJson;
        }

        ResourceLocation familyLoc = ResourceLocation.tryParse(familyId);
        if (familyLoc == null) {
            LOGGER.warn("Invalid family ID: {}", familyId);
            return skillJson;
        }

        var familyOpt = com.etema.ragnarmmo.skill.data.family.SkillFamilyRegistry.get(familyLoc);
        if (familyOpt.isEmpty()) {
            LOGGER.warn("Family not found: {}", familyId);
            return skillJson;
        }

        var family = familyOpt.get();
        JsonObject merged = new JsonObject();

        // Apply family defaults first
        for (String fieldName : family.getDefaultFields()) {
            Object value = family.getDefault(fieldName);
            if (value != null) {
                addToJson(merged, fieldName, value);
            }
        }

        // Then override with skill-specific values
        for (var entry : skillJson.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("family")) { // Don't copy the "family" field itself
                merged.add(key, entry.getValue());
            }
        }

        return merged;
    }

    /**
     * Add a Java object to a JsonObject with the appropriate type.
     */
    private void addToJson(JsonObject json, String key, Object value) {
        if (value instanceof String) {
            json.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            json.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            json.addProperty(key, (Boolean) value);
        } else if (value instanceof java.util.Map) {
            // Convert Map to JsonObject
            JsonObject obj = new JsonObject();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            for (var entry : map.entrySet()) {
                addToJson(obj, entry.getKey(), entry.getValue());
            }
            json.add(key, obj);
        } else if (value instanceof java.util.List) {
            // Convert List to JsonArray
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) value;
            for (Object item : list) {
                if (item instanceof String) {
                    arr.add((String) item);
                } else if (item instanceof Number) {
                    arr.add((Number) item);
                } else if (item instanceof Boolean) {
                    arr.add((Boolean) item);
                }
            }
            json.add(key, arr);
        }
    }

    // === Parsing helpers ===

    private SkillCategory parseCategory(String value) {
        try {
            return SkillCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SkillCategory.CLASS_PASSIVE;
        }
    }

    private SkillTier parseTier(String value) {
        try {
            return SkillTier.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SkillTier.FIRST;
        }
    }

    private SkillUsageType parseUsage(String value) {
        try {
            return SkillUsageType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SkillUsageType.PASSIVE;
        }
    }

    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }

    private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsInt();
        }
        return defaultValue;
    }

    private double getDoubleOrDefault(JsonObject json, String key, double defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsDouble();
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
            if (event.getPlayer() != null) {
                INSTANCE.syncToPlayer(event.getPlayer());
            } else {
                INSTANCE.syncToAll();
            }
        }
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
