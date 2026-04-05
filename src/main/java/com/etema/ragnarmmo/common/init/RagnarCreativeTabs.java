package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.DaggerItems;
import com.etema.ragnarmmo.roitems.ZenyItems;
import com.etema.ragnarmmo.system.loot.cards.RagnarCardItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Creative tabs for RagnarMMO.
 */
public final class RagnarCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            RagnarMMO.MODID);

    public static final RegistryObject<CreativeModeTab> RAGNAR_ITEMS_TAB = TABS.register("items",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ragnarmmo.items"))
                    .icon(() -> new ItemStack(ZenyItems.GOLD_ZENY.get()))
                    .displayItems((params, output) -> {
                        // Zeny
                        var copper = ZenyItems.COPPER_ZENY.get();
                        var silver = ZenyItems.SILVER_ZENY.get();
                        var gold = ZenyItems.GOLD_ZENY.get();
                        if (copper != null) output.accept(copper);
                        if (silver != null) output.accept(silver);
                        if (gold != null) output.accept(gold);

                        // Cards
                        var card = RagnarCardItems.CARD.get();
                        if (card != null) output.accept(card);
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> RAGNAR_WEAPONS_TAB = TABS.register("weapons",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ragnarmmo.weapons"))
                    .icon(() -> {
                        var icon = DaggerItems.GLACIAL_SHARD.get();
                        return new ItemStack(icon != null ? icon : Items.IRON_SWORD);
                    })
                    .displayItems((params, output) -> {
                        // Custom Daggers
                        output.accept(DaggerItems.GLACIAL_SHARD.get());
                        output.accept(DaggerItems.VOLCANIC_TOOTH.get());
                        output.accept(DaggerItems.GALE_DIRK.get());
                        output.accept(DaggerItems.QUAKE_STILETTO.get());
                        output.accept(DaggerItems.VEIN_SEEKER.get());
                        output.accept(DaggerItems.SHADOWS_EMBRACE.get());
                        output.accept(DaggerItems.MAMBAS_KISS.get());
                        output.accept(DaggerItems.GHOSTLY_STING.get());
                        output.accept(DaggerItems.VALKYRIES_THORN.get());
                        output.accept(DaggerItems.ABYSSAL_FANG.get());
                        output.accept(DaggerItems.TWILIGHT_CARVER.get());
                        output.accept(DaggerItems.OATHBREAKER.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private RagnarCreativeTabs() {
    }
}
