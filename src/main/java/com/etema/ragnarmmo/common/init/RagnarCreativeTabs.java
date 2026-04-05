package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.RagnarWeaponItems;
import com.etema.ragnarmmo.roitems.UtilityItems;
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
                        var blueGemstone = UtilityItems.BLUE_GEMSTONE.get();
                        var oridecon = UtilityItems.ORIDECON.get();
                        var elunium = UtilityItems.ELUNIUM.get();
                        if (copper != null) output.accept(copper);
                        if (silver != null) output.accept(silver);
                        if (gold != null) output.accept(gold);
                        if (blueGemstone != null) output.accept(blueGemstone);
                        if (oridecon != null) output.accept(oridecon);
                        if (elunium != null) output.accept(elunium);

                        // Cards
                        var card = RagnarCardItems.CARD.get();
                        if (card != null) output.accept(card);
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> RAGNAR_WEAPONS_TAB = TABS.register("weapons",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ragnarmmo.weapons"))
                    .icon(() -> {
                        var icon = RagnarWeaponItems.tabIcon() != null ? RagnarWeaponItems.tabIcon().get() : null;
                        return new ItemStack(icon != null ? icon : Items.IRON_SWORD);
                    })
                    .displayItems((params, output) -> RagnarWeaponItems.allWeapons().forEach(item -> output.accept(item.get())))
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private RagnarCreativeTabs() {
    }
}
