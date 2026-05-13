package com.etema.ragnarmmo.items;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class RagnarWeaponItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarCore.MODID);
    private static final List<RegistryObject<Item>> ALL_WEAPONS = new ArrayList<>();

    private RagnarWeaponItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static List<RegistryObject<Item>> allWeapons() {
        return List.copyOf(ALL_WEAPONS);
    }

    public static RegistryObject<Item> tabIcon() {
        return ALL_WEAPONS.isEmpty() ? null : ALL_WEAPONS.get(0);
    }

    static {
        ALL_WEAPONS.add(registerWeapon("weapons/sword1h/sword", "Sword", 4, -2.4F,
                "One-handed sword with a custom Blockbench model."));
        ALL_WEAPONS.add(registerWeapon("weapons/sword1h/falchion", "Falchion", 4, -2.4F,
                "One-handed falchion with a custom Blockbench model."));
        ALL_WEAPONS.add(registerWeapon("weapons/sword1h/blade", "Blade", 5, -2.4F,
                "Preview weapon using the custom Blockbench model."));
        ALL_WEAPONS.add(registerWeapon("weapons/sword1h/curved_sword", "Curved Sword", 8, -2.35F,
                "One-handed curved sword with a custom Blockbench model."));
        ALL_WEAPONS.add(registerWeapon("weapons/sword1h/byeollungum", "Byeollungum", 10, -2.5F,
                "One-handed legendary sword with a custom Blockbench model."));
    }

    private static RegistryObject<Item> registerWeapon(String id, String displayName, int attackDamage,
            float attackSpeed, String description) {
        return ITEMS.register(id,
                () -> new RagnarSwordLikeItem(Tiers.IRON, attackDamage, attackSpeed, new Item.Properties(), displayName,
                        description));
    }
}
