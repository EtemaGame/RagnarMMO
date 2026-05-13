package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.bestiary.api.BestiaryCategory;
import com.etema.ragnarmmo.bestiary.api.BestiaryDropInfoDto;
import com.etema.ragnarmmo.bestiary.api.BestiaryDropSource;
import com.etema.ragnarmmo.bestiary.api.BestiarySpawnInfoDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BestiaryDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    public static final BestiaryDataLoader INSTANCE = new BestiaryDataLoader();

    private BestiaryDataLoader() {
        super(GSON, "bestiary");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BestiaryOverride> overrides = new LinkedHashMap<>();
        List<BestiaryLoadIssue> issues = new ArrayList<>();
        int ignored = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation source = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null, "bestiary resource must be an object"));
                    ignored++;
                    continue;
                }
                BestiaryOverride override = parseOverride(source, entry.getValue().getAsJsonObject(), issues);
                if (override == null) {
                    ignored++;
                    continue;
                }
                BestiaryOverride previous = overrides.put(override.entityId(), override);
                if (previous != null) {
                    issues.add(issue(BestiaryLoadIssue.Kind.DUPLICATE, source, override.entityId(),
                            "duplicate bestiary metadata; higher-priority resource overrides " + previous.source()));
                }
            } catch (Exception ex) {
                issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null, ex.getMessage()));
                ignored++;
            }
        }

        BestiaryRegistry.getInstance().replaceOverrides(overrides, issues);
        RagnarMMO.LOGGER.info("Bestiary metadata summary: loaded={}, ignored={}, issues={}",
                overrides.size(), ignored, issues.size());
    }

    private static BestiaryOverride parseOverride(
            ResourceLocation source,
            JsonObject json,
            List<BestiaryLoadIssue> issues) {
        int schemaVersion = getRequiredInt(json, "schema_version", source, issues);
        if (schemaVersion != 1) {
            issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null,
                    "unsupported or missing schema_version; expected 1"));
            return null;
        }

        ResourceLocation entityId = parseRequiredId(json, "entity", source, issues);
        if (entityId == null) {
            return null;
        }
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            issues.add(issue(BestiaryLoadIssue.Kind.MISSING_REGISTRY_ENTRY, source, entityId,
                    "entity is not registered: " + entityId));
            return null;
        }

        Optional<BestiaryCategory> category = Optional.empty();
        if (json.has("category")) {
            try {
                category = Optional.of(BestiaryCategory.valueOf(json.get("category").getAsString().toUpperCase(java.util.Locale.ROOT)));
            } catch (Exception ex) {
                issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId,
                        "invalid category: " + json.get("category")));
                return null;
            }
        }

        boolean visible = !json.has("visible") || json.get("visible").getAsBoolean();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        Optional<BestiarySpawnInfoDto> spawn = json.has("spawn")
                ? parseSpawn(source, entityId, json.getAsJsonObject("spawn"), issues)
                : Optional.empty();
        List<BestiaryDropInfoDto> drops = json.has("drops")
                ? parseDrops(source, entityId, json.getAsJsonArray("drops"), issues)
                : List.of();

        return new BestiaryOverride(entityId, category, visible, description, spawn, drops, source);
    }

    private static Optional<BestiarySpawnInfoDto> parseSpawn(
            ResourceLocation source,
            ResourceLocation entityId,
            JsonObject json,
            List<BestiaryLoadIssue> issues) {
        List<ResourceLocation> dimensions = new ArrayList<>();
        if (json.has("dimensions")) {
            JsonArray array = json.getAsJsonArray("dimensions");
            for (JsonElement element : array) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id == null) {
                    issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId,
                            "invalid spawn dimension id: " + element));
                    continue;
                }
                dimensions.add(id);
            }
        }
        String notes = json.has("notes") ? json.get("notes").getAsString() : "";
        return Optional.of(new BestiarySpawnInfoDto(dimensions, notes));
    }

    private static List<BestiaryDropInfoDto> parseDrops(
            ResourceLocation source,
            ResourceLocation entityId,
            JsonArray array,
            List<BestiaryLoadIssue> issues) {
        List<BestiaryDropInfoDto> drops = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId, "drop entry must be an object"));
                continue;
            }
            JsonObject json = element.getAsJsonObject();
            ResourceLocation itemId = parseRequiredId(json, "item", source, issues);
            if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
                issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId,
                        "drop item is not registered: " + itemId));
                continue;
            }
            BestiaryDropSource dropSource = BestiaryDropSource.DOCUMENTED;
            if (json.has("source")) {
                try {
                    dropSource = BestiaryDropSource.valueOf(json.get("source").getAsString().toUpperCase(java.util.Locale.ROOT));
                } catch (Exception ex) {
                    issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId,
                            "invalid drop source: " + json.get("source")));
                    continue;
                }
            }
            int min = json.has("min") ? json.get("min").getAsInt() : 0;
            int max = json.has("max") ? json.get("max").getAsInt() : Math.max(1, min);
            double chance = json.has("chance") ? json.get("chance").getAsDouble() : 0.0D;
            if (min < 0 || max < min || chance < 0.0D || chance > 1.0D) {
                issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, entityId,
                        "invalid drop min/max/chance for item " + itemId));
                continue;
            }
            String label = json.has("label") ? json.get("label").getAsString() : "";
            String noteId = json.has("note") ? json.get("note").getAsString() : "";
            drops.add(new BestiaryDropInfoDto(itemId, min, max, chance, dropSource, label, noteId));
        }
        return drops;
    }

    private static int getRequiredInt(JsonObject json, String field, ResourceLocation source, List<BestiaryLoadIssue> issues) {
        if (!json.has(field)) {
            issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null, "missing required field: " + field));
            return -1;
        }
        return json.get(field).getAsInt();
    }

    private static ResourceLocation parseRequiredId(
            JsonObject json,
            String field,
            ResourceLocation source,
            List<BestiaryLoadIssue> issues) {
        if (!json.has(field)) {
            issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null, "missing required field: " + field));
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(json.get(field).getAsString());
        if (id == null) {
            issues.add(issue(BestiaryLoadIssue.Kind.INVALID, source, null, "invalid ResourceLocation in field: " + field));
        }
        return id;
    }

    private static BestiaryLoadIssue issue(
            BestiaryLoadIssue.Kind kind,
            ResourceLocation source,
            ResourceLocation entityId,
            String message) {
        return new BestiaryLoadIssue(kind, source, entityId, message);
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                BestiaryRegistry.getInstance().syncToPlayer(event.getPlayer());
            } else if (event.getPlayerList() != null) {
                for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                    BestiaryRegistry.getInstance().syncToPlayer(player);
                }
            }
        }
    }
}
