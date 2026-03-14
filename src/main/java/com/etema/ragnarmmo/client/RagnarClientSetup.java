package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.TestItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RagnarClientSetup {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerShieldProperty(TestItems.TEST_SHIELD.get());
        });
    }

    private static void registerShieldProperty(Item shield) {
        ItemProperties.register(shield, new ResourceLocation("blocking"),
                (stack, world, entity,
                        seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hotbar_overlay", com.etema.ragnarmmo.client.ui.HotbarOverlay.INSTANCE);
    }
}
