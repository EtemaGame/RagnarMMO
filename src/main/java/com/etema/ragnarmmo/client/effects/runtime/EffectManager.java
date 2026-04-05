package com.etema.ragnarmmo.client.effects.runtime;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.client.effects.SkillEffectRegistry;
import com.etema.ragnarmmo.client.effects.render.SkillEffectRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EffectManager {
    private static final EffectManager INSTANCE = new EffectManager();
    private final Map<UUID, EffectInstance> activeEffects = new LinkedHashMap<>();

    private EffectManager() {
    }

    public static EffectManager get() {
        return INSTANCE;
    }

    public Optional<EffectInstance> spawnAttachedEffect(Entity entity, net.minecraft.resources.ResourceLocation effectId,
            EffectContext context) {
        return SkillEffectRegistry.get(effectId).map(definition -> {
            EffectInstance instance = new EffectInstance(definition, new EntityEffectAnchor(entity), context);
            activeEffects.put(instance.id(), instance);
            SkillEffectRuntimeDispatcher.onSpawn(instance, Minecraft.getInstance().level);
            return instance;
        });
    }

    public Optional<EffectInstance> spawnWorldEffect(Vec3 position, net.minecraft.resources.ResourceLocation effectId,
            EffectContext context) {
        return SkillEffectRegistry.get(effectId).map(definition -> {
            EffectInstance instance = new EffectInstance(definition, new WorldEffectAnchor(position), context);
            activeEffects.put(instance.id(), instance);
            SkillEffectRuntimeDispatcher.onSpawn(instance, Minecraft.getInstance().level);
            return instance;
        });
    }

    public Optional<EffectInstance> ensureAttachedEffect(Entity entity, net.minecraft.resources.ResourceLocation effectId,
            EffectContext context) {
        for (EffectInstance activeEffect : activeEffects.values()) {
            if (activeEffect.definition().id().equals(effectId) && activeEffect.anchor().entity() == entity) {
                return Optional.of(activeEffect);
            }
        }
        return spawnAttachedEffect(entity, effectId, context);
    }

    public void clear() {
        activeEffects.clear();
    }

    private void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            clear();
            return;
        }

        Iterator<EffectInstance> iterator = activeEffects.values().iterator();
        while (iterator.hasNext()) {
            EffectInstance instance = iterator.next();
            if (!instance.anchor().isAlive()) {
                iterator.remove();
                continue;
            }
            SkillEffectRuntimeDispatcher.tick(instance, minecraft.level);
            instance.playbackState().tick();
            if (instance.isExpired()) {
                iterator.remove();
            }
        }
    }

    private void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || activeEffects.isEmpty()) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (EffectInstance instance : activeEffects.values()) {
            Vec3 anchorPos = instance.anchor().resolvePosition(event.getPartialTick());
            poseStack.pushPose();
            poseStack.translate(anchorPos.x - cameraPos.x, anchorPos.y - cameraPos.y, anchorPos.z - cameraPos.z);
            SkillEffectRenderDispatcher.renderAtCurrentOrigin(instance, poseStack, bufferSource, 0x00F000F0,
                    event.getPartialTick());
            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                EffectManager.get().tick(Minecraft.getInstance());
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            EffectManager.get().render(event);
        }
    }
}
