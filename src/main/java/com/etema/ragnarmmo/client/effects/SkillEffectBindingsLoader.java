package com.etema.ragnarmmo.client.effects;

import com.etema.ragnarmmo.RagnarMMO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkillEffectBindingsLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectBindingsLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final SkillEffectBindingsLoader INSTANCE = new SkillEffectBindingsLoader();

    private SkillEffectBindingsLoader() {
        super(GSON, "effect_bindings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("skill_effect_bindings");
        SkillEffectBindingsRegistry.clear();

        int loaded = 0;
        int failed = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                failed++;
                continue;
            }

            try {
                SkillEffectBindings bindings = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                SkillEffectBindingsRegistry.register(bindings);
                loaded++;
            } catch (Exception exception) {
                failed++;
                LOGGER.warn("Failed to load skill effect binding {}: {}", entry.getKey(), exception.getMessage());
            }
        }

        LOGGER.info("Loaded {} skill effect binding files ({} failed)", loaded, failed);
        profiler.pop();
    }

    private SkillEffectBindings parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation skillId = json.has("skill_id")
                ? ResourceLocation.tryParse(GsonHelper.getAsString(json, "skill_id"))
                : ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath());
        if (skillId == null) {
            throw new IllegalArgumentException("Invalid skill_id in " + fileId);
        }

        List<SkillEffectBindings.BindingEntry> bindings = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "bindings")) {
            JsonObject binding = element.getAsJsonObject();
            bindings.add(new SkillEffectBindings.BindingEntry(
                    EffectTriggerPhase.valueOf(GsonHelper.getAsString(binding, "phase").toUpperCase(Locale.ROOT)),
                    ResourceLocation.tryParse(GsonHelper.getAsString(binding, "effect"))));
        }

        return new SkillEffectBindings(skillId, List.copyOf(bindings));
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        @SubscribeEvent
        public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
