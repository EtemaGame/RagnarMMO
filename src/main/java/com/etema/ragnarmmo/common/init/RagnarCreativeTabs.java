package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.TestItems;
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
                        var icon = TestItems.TEST_SWORD_1H.get();
                        return new ItemStack(icon != null ? icon : Items.IRON_SWORD);
                    })
                    .displayItems((params, output) -> {
                        // Swords
                        var sword1h = TestItems.TEST_SWORD_1H.get();
                        var sword2h = TestItems.TEST_SWORD_2H.get();
                        var dagger = TestItems.TEST_DAGGER.get();
                        if (sword1h != null) output.accept(sword1h);
                        if (sword2h != null) output.accept(sword2h);
                        if (dagger != null) output.accept(dagger);

                        // Maces & Axes
                        var mace1h = TestItems.TEST_MACE_1H.get();
                        var mace2h = TestItems.TEST_MACE_2H.get();
                        var waraxe1h = TestItems.TEST_WARAXE_1H.get();
                        var waraxe2h = TestItems.TEST_WARAXE_2H.get();
                        if (mace1h != null) output.accept(mace1h);
                        if (mace2h != null) output.accept(mace2h);
                        if (waraxe1h != null) output.accept(waraxe1h);
                        if (waraxe2h != null) output.accept(waraxe2h);

                        // Magic
                        var wand = TestItems.TEST_WAND.get();
                        var staff = TestItems.TEST_STAFF.get();
                        if (wand != null) output.accept(wand);
                        if (staff != null) output.accept(staff);

                        // Shield
                        var shield = TestItems.TEST_SHIELD.get();
                        if (shield != null) output.accept(shield);
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private RagnarCreativeTabs() {
    }
}
