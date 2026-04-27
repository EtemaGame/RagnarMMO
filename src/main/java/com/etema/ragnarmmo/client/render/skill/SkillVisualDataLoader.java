package com.etema.ragnarmmo.client.render.skill;

import com.etema.ragnarmmo.RagnarMMO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * Loads client-side skill visual declarations from assets so resourcepacks can override them.
 */
public final class SkillVisualDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillVisualDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final SkillVisualDataLoader INSTANCE = new SkillVisualDataLoader();

    private SkillVisualDataLoader() {
        super(GSON, "skill_visuals");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        profiler.push("skill_visuals");
        SkillVisualsRegistry.clear();

        int loaded = 0;
        int failed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                failed++;
                continue;
            }

            try {
                parse(entry.getKey(), entry.getValue().getAsJsonObject());
                loaded++;
            } catch (Exception e) {
                failed++;
                LOGGER.warn("Failed to load skill visual {}: {}", entry.getKey(), e.getMessage());
            }
        }

        LOGGER.info("Loaded {} skill visual declarations ({} failed)", loaded, failed);
        profiler.pop();
    }

    private void parse(ResourceLocation fileId, JsonObject json) {
        ResourceLocation skillId = parseResourceLocation(getStringOrDefault(json, "skill_id", null));
        if (skillId == null) {
            skillId = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath());
        }

        String typeValue = getStringOrDefault(json, "type", "sprite").toUpperCase(Locale.ROOT);
        SkillVisuals.VisualType visualType = SkillVisuals.VisualType.valueOf(typeValue);

        String textureStr = getStringOrDefault(json, "texture", null);
        ResourceLocation texture = textureStr != null
                ? parseResourceLocation(textureStr)
                : defaultTextureFor(visualType);

        SkillVisuals.Builder builder = SkillVisuals.builder(texture)
                .columns(getIntOrDefault(json, "columns", 1))
                .rows(getIntOrDefault(json, "rows", 1))
                .size((float) getDoubleOrDefault(json, "size", 1.0))
                .animationFPS(getIntOrDefault(json, "animation_fps", 20))
                .billboard(getBooleanOrDefault(json, "billboard", true))
                .renderType(parseRenderType(getStringOrDefault(json, "render_type", "translucent"), texture));

        builder.type(visualType);

        if (json.has("block")) {
            BlockState state = parseBlockState(getStringOrDefault(json, "block", null));
            if (state != null) {
                builder.block(state);
            }
        }

        String modelStr = getStringOrDefault(json, "model", null);
        if (modelStr != null) {
            ResourceLocation model = parseResourceLocation(modelStr);
            if (model != null) {
                builder.model(model);
            }
        }

        SkillVisualsRegistry.register(skillId, builder.build());
    }

    private ResourceLocation defaultTextureFor(SkillVisuals.VisualType visualType) {
        return switch (visualType) {
            case BLOCK, MODEL, SPRITE -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
        };
    }

    private BlockState parseBlockState(String blockId) {
        ResourceLocation blockLoc = parseResourceLocation(blockId);
        if (blockLoc == null) {
            return null;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(blockLoc);
        return block != null ? block.defaultBlockState() : null;
    }

    private RenderType parseRenderType(String value, ResourceLocation texture) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "translucent_emissive" -> RenderType.entityTranslucentEmissive(texture);
            case "cutout", "cutout_no_cull" -> RenderType.entityCutoutNoCull(texture);
            default -> RenderType.entityTranslucent(texture);
        };
    }

    private ResourceLocation parseResourceLocation(String value) {
        return value != null && !value.isBlank() ? ResourceLocation.tryParse(value) : null;
    }

    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : defaultValue;
    }

    private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : defaultValue;
    }

    private double getDoubleOrDefault(JsonObject json, String key, double defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : defaultValue;
    }

    private boolean getBooleanOrDefault(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : defaultValue;
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Events {
        @SubscribeEvent
        public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
