package com.etema.ragnarmmo.client.effects;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.client.effects.importer.RoStrAdapter;
import com.etema.ragnarmmo.client.effects.importer.RoStrParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkillEffectLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final SkillEffectLoader INSTANCE = new SkillEffectLoader();

    private SkillEffectLoader() {
        super(GSON, "effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("skill_effects");
        SkillEffectRegistry.clear();

        int loaded = 0;
        int failed = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                failed++;
                continue;
            }

            try {
                SkillEffectDefinition definition = parseDefinition(entry.getKey(), entry.getValue().getAsJsonObject(),
                        resourceManager);
                SkillEffectRegistry.register(definition);
                loaded++;
            } catch (Exception exception) {
                failed++;
                LOGGER.warn("Failed to load skill effect {}: {}", entry.getKey(), exception.getMessage());
            }
        }

        LOGGER.info("Loaded {} client skill effects ({} failed)", loaded, failed);
        profiler.pop();
    }

    private SkillEffectDefinition parseDefinition(ResourceLocation fileId, JsonObject json, ResourceManager resourceManager)
            throws IOException {
        ResourceLocation id = parseId(json, fileId);
        SkillEffectType type = SkillEffectType.valueOf(
                GsonHelper.getAsString(json, "type", "sprite_sheet").toUpperCase(Locale.ROOT));
        return switch (type) {
            case SPRITE_SHEET -> parseSpriteSheet(id, json);
            case LAYERED -> parseLayered(id, json);
            case COMPOSITE -> parseComposite(id, json);
            case STR_LAYERED -> parseStrLayered(id, json, resourceManager);
            case PARTICLE_EMITTER -> parseParticleEmitter(id, json);
            default -> throw new IOException("Unsupported effect type in MVP: " + type);
        };
    }

    private SpriteSheetEffectDefinition parseSpriteSheet(ResourceLocation id, JsonObject json) {
        int columns = GsonHelper.getAsInt(json, "columns", 1);
        int rows = GsonHelper.getAsInt(json, "rows", 1);
        int frameCount = GsonHelper.getAsInt(json, "frame_count", Math.max(1, columns * rows));
        int fps = GsonHelper.getAsInt(json, "fps", GsonHelper.getAsInt(json, "animation_fps", 20));
        int durationTicks = GsonHelper.getAsInt(json, "duration_ticks",
                Math.max(1, (int) Math.ceil((frameCount * 20.0) / Math.max(1, fps))));
        boolean billboard = GsonHelper.getAsBoolean(json, "billboard", true);

        return new SpriteSheetEffectDefinition(
                id,
                durationTicks,
                GsonHelper.getAsBoolean(json, "loop", true),
                parseTexture(GsonHelper.getAsString(json, "texture")),
                columns,
                rows,
                frameCount,
                fps,
                (float) GsonHelper.getAsDouble(json, "size", 1.0),
                parseOrientation(json, billboard),
                parseVec3(json, "offset", EffectVec3.ZERO),
                parseBlendMode(GsonHelper.getAsString(json, "render_type", "translucent")));
    }

    private LayeredEffectDefinition parseLayered(ResourceLocation id, JsonObject json) {
        List<LayeredEffectDefinition.LayerDefinition> layers = new ArrayList<>();
        int maxTick = 1;

        JsonArray jsonLayers = GsonHelper.getAsJsonArray(json, "layers");
        for (int layerIndex = 0; layerIndex < jsonLayers.size(); layerIndex++) {
            JsonObject layerJson = jsonLayers.get(layerIndex).getAsJsonObject();
            List<LayeredEffectDefinition.KeyframeDefinition> keyframes = new ArrayList<>();
            JsonArray jsonKeyframes = GsonHelper.getAsJsonArray(layerJson, "keyframes");

            ResourceLocation layerTexture = layerJson.has("texture")
                    ? parseTexture(GsonHelper.getAsString(layerJson, "texture"))
                    : null;

            for (JsonElement keyframeElement : jsonKeyframes) {
                JsonObject keyframeJson = keyframeElement.getAsJsonObject();
                int tick = GsonHelper.getAsInt(keyframeJson, "tick");
                maxTick = Math.max(maxTick, tick);
                keyframes.add(new LayeredEffectDefinition.KeyframeDefinition(
                        tick,
                        (float) GsonHelper.getAsDouble(keyframeJson, "x", 0.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "y", 0.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "z", 0.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "scale_x", 1.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "scale_y", 1.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "rotation_deg", 0.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "alpha", 1.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "r", 1.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "g", 1.0),
                        (float) GsonHelper.getAsDouble(keyframeJson, "b", 1.0),
                        keyframeJson.has("texture")
                                ? parseTexture(GsonHelper.getAsString(keyframeJson, "texture"))
                                : layerTexture));
            }

            keyframes.sort(Comparator.comparingInt(LayeredEffectDefinition.KeyframeDefinition::tick));
            layers.add(new LayeredEffectDefinition.LayerDefinition(
                    GsonHelper.getAsString(layerJson, "name", "layer_" + layerIndex),
                    parseBlendMode(GsonHelper.getAsString(layerJson, "blend_mode", "translucent")),
                    parseOrientation(layerJson, GsonHelper.getAsBoolean(layerJson, "billboard", true)),
                    GsonHelper.getAsInt(layerJson, "z_order", layerIndex),
                    List.copyOf(keyframes)));
        }

        int durationTicks = GsonHelper.getAsInt(json, "duration_ticks", maxTick + 1);
        return new LayeredEffectDefinition(id, durationTicks, GsonHelper.getAsBoolean(json, "loop", false),
                List.copyOf(layers));
    }

    private CompositeEffectDefinition parseComposite(ResourceLocation id, JsonObject json) {
        List<CompositeEffectDefinition.ChildDefinition> children = new ArrayList<>();
        JsonArray jsonChildren = GsonHelper.getAsJsonArray(json, "children");

        for (JsonElement childElement : jsonChildren) {
            JsonObject childJson = childElement.getAsJsonObject();
            children.add(new CompositeEffectDefinition.ChildDefinition(
                    ResourceLocation.tryParse(GsonHelper.getAsString(childJson, "effect")),
                    GsonHelper.getAsInt(childJson, "start_tick", 0),
                    parseVec3(childJson, "offset", EffectVec3.ZERO),
                    (float) GsonHelper.getAsDouble(childJson, "scale", 1.0),
                    parseColor(childJson, "tint", EffectColor.WHITE)));
        }

        return new CompositeEffectDefinition(
                id,
                GsonHelper.getAsInt(json, "duration_ticks", 20),
                GsonHelper.getAsBoolean(json, "loop", false),
                List.copyOf(children));
    }

    private ParticleEmitterEffectDefinition parseParticleEmitter(ResourceLocation id, JsonObject json) {
        return new ParticleEmitterEffectDefinition(
                id,
                GsonHelper.getAsInt(json, "duration_ticks", 20),
                GsonHelper.getAsBoolean(json, "loop", false),
                parseTexture(GsonHelper.getAsString(json, "particle")),
                ParticleEmitterShape.parse(GsonHelper.getAsString(json, "shape", "point")),
                Math.max(1, GsonHelper.getAsInt(json, "emit_interval_ticks", 1)),
                Math.max(1, GsonHelper.getAsInt(json, "count", 1)),
                (float) GsonHelper.getAsDouble(json, "radius", 0.0),
                (float) GsonHelper.getAsDouble(json, "radial_velocity", 0.0),
                (float) GsonHelper.getAsDouble(json, "rotation_per_tick_deg", 0.0),
                (float) GsonHelper.getAsDouble(json, "inherit_entity_velocity", 0.0),
                parseVec3(json, "offset", EffectVec3.ZERO),
                parseVec3(json, "spread", EffectVec3.ZERO),
                parseVec3(json, "base_velocity", EffectVec3.ZERO),
                parseVec3(json, "random_velocity", EffectVec3.ZERO));
    }

    private StrLayeredEffectDefinition parseStrLayered(ResourceLocation id, JsonObject json, ResourceManager resourceManager)
            throws IOException {
        ResourceLocation source = ResourceLocation.tryParse(GsonHelper.getAsString(json, "source"));
        if (source == null) {
            throw new IOException("Invalid STR source for " + id);
        }

        Resource resource = resourceManager.getResource(source)
                .orElseThrow(() -> new IOException("Missing STR source " + source));
        byte[] bytes;
        try (var stream = resource.open()) {
            bytes = stream.readAllBytes();
        }

        RoStrParser parser = new RoStrParser();
        RoStrAdapter adapter = new RoStrAdapter();
        RoStrParser.RoStrEffect effect = parser.parse(bytes);
        ResourceLocation textureNamespace = ResourceLocation.fromNamespaceAndPath(
                GsonHelper.getAsString(json, "texture_namespace", id.getNamespace()), "placeholder");

        return adapter.adapt(
                id,
                effect,
                textureNamespace,
                GsonHelper.getAsString(json, "texture_base_path", "ro"),
                parseOrientation(json, true),
                GsonHelper.getAsInt(json, "duration_ticks", -1),
                GsonHelper.getAsBoolean(json, "loop", false));
    }

    private ResourceLocation parseId(JsonObject json, ResourceLocation fileId) {
        if (json.has("id")) {
            return ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
        }
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath());
    }

    private ResourceLocation parseTexture(String texture) {
        ResourceLocation parsed = ResourceLocation.tryParse(texture);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid texture resource location: " + texture);
        }
        return parsed;
    }

    private EffectOrientation parseOrientation(JsonObject json, boolean billboardFallback) {
        if (json.has("orientation")) {
            return EffectOrientation.valueOf(GsonHelper.getAsString(json, "orientation").toUpperCase(Locale.ROOT));
        }
        return billboardFallback ? EffectOrientation.BILLBOARD : EffectOrientation.GROUND;
    }

    private EffectVec3 parseVec3(JsonObject json, String key, EffectVec3 fallback) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray array = json.getAsJsonArray(key);
        if (array.size() < 3) {
            return fallback;
        }
        return new EffectVec3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private EffectColor parseColor(JsonObject json, String key, EffectColor fallback) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray array = json.getAsJsonArray(key);
        if (array.size() < 4) {
            return fallback;
        }
        return new EffectColor(array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }

    private BlendMode parseBlendMode(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "cutout", "cutout_no_cull" -> BlendMode.CUTOUT;
            case "translucent_emissive", "emissive" -> BlendMode.EMISSIVE;
            case "additive" -> BlendMode.ADDITIVE;
            default -> BlendMode.TRANSLUCENT;
        };
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        @SubscribeEvent
        public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
