package com.etema.ragnarmmo.client.render;

import com.etema.ragnarmmo.common.init.RagnarEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ragnarmmo", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RagnarEntities.MAGIC_PROJECTILE.get(), MagicProjectileRenderer::new);
    }
}
