package com.etema.ragnarmmo.achievements.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and registers Achievement definitions from datapacks.
 * Data is loaded from `data/[namespace]/achievements/`.
 */
public class AchievementRegistry extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER = "achievements";

    private static final AchievementRegistry INSTANCE = new AchievementRegistry();

    private final Map<String, AchievementDefinition> definitions = new ConcurrentHashMap<>();

    private AchievementRegistry() {
        super(GSON, FOLDER);
    }

    public static AchievementRegistry getInstance() {
        return INSTANCE;
    }

    public AchievementDefinition get(String id) {
        return definitions.get(id);
    }

    public Map<String, AchievementDefinition> getAll() {
        return Collections.unmodifiableMap(definitions);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        definitions.clear();

        jsonMap.forEach((location, element) -> {
            try {
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    String id = location.getPath();

                    AchievementCategory category = AchievementCategory.BASIC;
                    if (json.has("category")) {
                        category = AchievementCategory.fromString(json.get("category").getAsString());
                    }

                    String name = json.has("name") ? json.get("name").getAsString() : id;
                    String desc = json.has("description") ? json.get("description").getAsString() : "";

                    String triggerType = json.has("trigger_type") ? json.get("trigger_type").getAsString() : json.has("triggerType") ? json.get("triggerType").getAsString() : "unknown";
                    String triggerId = json.has("trigger_id") ? json.get("trigger_id").getAsString() : json.has("triggerId") ? json.get("triggerId").getAsString() : null;
                    int requiredAmt = json.has("required_amount") ? json.get("required_amount").getAsInt() : json.has("requiredAmount") ? json.get("requiredAmount").getAsInt() : 1;
                    int points = json.has("points") ? json.get("points").getAsInt() : 0;
                    String title = json.has("title") ? json.get("title").getAsString() : null;

                    Map<String, Integer> rewards = new HashMap<>();
                    if (json.has("rewards") && json.get("rewards").isJsonObject()) {
                        JsonObject rewardsJson = json.getAsJsonObject("rewards");
                        rewardsJson.entrySet().forEach(entry -> {
                            rewards.put(entry.getKey(), entry.getValue().getAsInt());
                        });
                    }

                    AchievementDefinition def = new AchievementDefinition(id, category, name, desc, triggerType,
                            triggerId, requiredAmt, points, title, rewards);
                    definitions.put(id, def);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse achievement JSON: {}", location, e);
            }
        });

        LOGGER.info("Loaded {} achievement definitions.", definitions.size());
    }

    /**
     * Apply synchronized definitions from server.
     * Used only on client.
     */
    public static void applySync(Collection<AchievementDefinition> syncedDefinitions) {
        INSTANCE.definitions.clear();
        for (AchievementDefinition def : syncedDefinitions) {
            INSTANCE.definitions.put(def.id(), def);
        }
        LOGGER.info("Applied {} synchronized achievement definitions from server", syncedDefinitions.size());
    }

    public void syncToAll() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        var packet = new com.etema.ragnarmmo.achievements.network.SyncAchievementDefinitionsPacket(definitions.values());
        for (var player : server.getPlayerList().getPlayers()) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, packet);
        }
    }

    public void syncToPlayer(net.minecraft.server.level.ServerPlayer player) {
        var packet = new com.etema.ragnarmmo.achievements.network.SyncAchievementDefinitionsPacket(definitions.values());
        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, packet);
    }
}
