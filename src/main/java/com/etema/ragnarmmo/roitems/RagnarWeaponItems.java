package com.etema.ragnarmmo.roitems;

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

    private static RegistryObject<Item> registerDagger(String id, String name, String description, int attackDamage,
            float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register("daggers/" + id,
                () -> new RagnarDaggerItem(Tiers.IRON, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerSword(String folder, String id, String name, String description,
            int attackDamage, float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register(folder + "/" + id,
                () -> new RagnarSwordLikeItem(Tiers.IRON, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerWand(String id, String name, String description, int attackDamage,
            float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register("wands/" + id,
                () -> new RagnarSwordLikeItem(Tiers.WOOD, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerStaff(String id, String name, String description, int attackDamage,
            float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register("staves/" + id,
                () -> new RagnarSwordLikeItem(Tiers.STONE, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerMace(String folder, String id, String name, String description,
            int attackDamage, float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register(folder + "/" + id,
                () -> new RagnarSwordLikeItem(Tiers.IRON, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerWarAxe(String id, String name, String description, float attackDamage,
            float attackSpeed) {
        RegistryObject<Item> item = ITEMS.register("war_axes_1h/" + id,
                () -> new RagnarAxeWeaponItem(Tiers.IRON, attackDamage, attackSpeed, new Item.Properties(), name,
                        description));
        ALL_WEAPONS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerBow(String id, String name, String description,
            double rangedWeaponAtk, int baseRangedAspd, int baseDrawTicks, float projectileVelocity) {
        RegistryObject<Item> item = ITEMS.register("bows/" + id,
                () -> new RagnarBowWeaponItem(new Item.Properties().stacksTo(1), name, description,
                        rangedWeaponAtk, baseRangedAspd, baseDrawTicks, projectileVelocity));
        ALL_WEAPONS.add(item);
        return item;
    }

    static {
        registerDaggers();
        registerOneHandedSwords();
        registerOneHandedMaces();
        registerWands();
        registerStaves();
        registerOneHandedWarAxes();
        registerBows();
    }

    private static void registerDaggers() {
        registerDagger("glacial_shard", "Glacial Shard",
                "A knife of frozen crystal (Lv 1).", 2, -1.5F);
        registerDagger("volcanic_tooth", "Volcanic Tooth",
                "A jagged fire dagger (Lv 15).", 4, -1.6F);
        registerDagger("gale_dirk", "Gale Dirk",
                "A light wind-imbued dagger (Lv 20).", 5, -1.2F);
        registerDagger("quake_stiletto", "Quake Stiletto",
                "A heavy earth dagger (Lv 25).", 7, -1.8F);
        registerDagger("vein_seeker", "Vein Seeker",
                "A thin blade that seeks blood (Lv 28).", 8, -1.5F);
        registerDagger("shadows_embrace", "Shadow's Embrace",
                "A dark-edge blade (Lv 30).", 6, -1.4F);
        registerDagger("mambas_kiss", "Mamba's Kiss",
                "A venomous tooth (Lv 35).", 9, -1.3F);
        registerDagger("ghostly_sting", "Ghostly Sting",
                "A spectral piercing blade (Lv 40).", 11, -1.7F);
        registerDagger("valkyries_thorn", "Valkyrie's Thorn",
                "A divine holy dagger (Lv 45).", 13, -1.4F);
        registerDagger("abyssal_fang", "Abyssal Fang",
                "A cursed blade from the void (Lv 50).", 15, -1.6F);
        registerDagger("twilight_carver", "Twilight Carver",
                "A balance of light and dark (Lv 55).", 18, -1.5F);
        registerDagger("oathbreaker", "Oathbreaker",
                "A legendary dagger for those who defy fate (Lv 60).", 22, -1.4F);
    }

    private static void registerOneHandedSwords() {
        registerSword("swords_1h", "ashen_rapier", "Ashen Rapier",
                "A lean dueling sword (Lv 1).", 4, -2.1F);
        registerSword("swords_1h", "frostwake_blade", "Frostwake Blade",
                "A cold-edged sword (Lv 15).", 6, -2.3F);
        registerSword("swords_1h", "sunspire_blade", "Sunspire Blade",
                "A radiant sword (Lv 30).", 8, -2.4F);
    }

    private static void registerOneHandedMaces() {
        registerMace("maces_1h", "grave_blackjack", "Grave Blackjack",
                "A compact iron beater (Lv 1).", 5, -2.5F);
        registerMace("maces_1h", "meteor_star", "Meteor Star",
                "A spiked head (Lv 15).", 7, -2.8F);
        registerMace("maces_1h", "gilden_comet", "Gilded Comet",
                "A decorated flanged mace (Lv 30).", 9, -2.7F);
    }

    private static void registerWands() {
        registerWand("moonring_wand", "Moonring Wand",
                "A slender wand (Lv 1).", 1, -1.8F);
        registerWand("suncoil_wand", "Suncoil Wand",
                "A warm golden conductor (Lv 15).", 2, -2.0F);
        registerWand("runebriar_wand", "Runebriar Wand",
                "A runed briar focus (Lv 30).", 3, -1.9F);
    }

    private static void registerStaves() {
        registerStaff("sunpriest_staff", "Sunpriest Staff",
                "A ceremonial staff (Lv 1).", 3, -2.3F);
        registerStaff("embervine_staff", "Embervine Staff",
                "A fire-laced staff (Lv 15).", 5, -2.4F);
        registerStaff("moonlily_staff", "Moonlily Staff",
                "A lunar staff (Lv 30).", 7, -2.5F);
    }

    private static void registerOneHandedWarAxes() {
        registerWarAxe("sunmaw_axe", "Sunmaw Axe",
                "A bright war axe (Lv 1).", 6.0F, -3.0F);
        registerWarAxe("stormsplitter", "Stormsplitter",
                "A twin-edged axe (Lv 15).", 8.0F, -3.0F);
        registerWarAxe("scarlet_lopper", "Scarlet Lopper",
                "A brutal red axe (Lv 30).", 10.5F, -3.1F);
    }

    private static void registerBows() {
        registerBow("wooden_shortbow", "Wooden Shortbow",
                "A simple bow for beginners (Lv 1).", 18, 174, 18, 2.8F);
        registerBow("composite_bow", "Composite Bow",
                "A reinforced bow (Lv 15).", 32, 170, 22, 3.0F);
        registerBow("hunters_longbow", "Hunter's Longbow",
                "A professional hunting bow (Lv 30).", 48, 166, 26, 3.15F);
    }
}
