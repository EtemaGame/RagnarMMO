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

    static {
        registerDaggers();
        registerOneHandedSwords();
        registerOneHandedMaces();
        registerWands();
        registerStaves();
        registerOneHandedWarAxes();
    }

    private static void registerDaggers() {
        registerDagger("glacial_shard", "Glacial Shard",
                "A knife of frozen crystal that rewards precise, patient strikes.", 2, -1.5F);
        registerDagger("volcanic_tooth", "Volcanic Tooth",
                "A jagged fang-tempered dagger that favors relentless close pressure.", 3, -1.6F);
        registerDagger("gale_dirk", "Gale Dirk",
                "A light dirk balanced for quick footwork and rapid follow-ups.", 2, -1.3F);
        registerDagger("quake_stiletto", "Quake Stiletto",
                "A dense thrusting blade built to punch through guard and armor seams.", 3, -1.6F);
        registerDagger("vein_seeker", "Vein Seeker",
                "A surgical dagger made for hunters who value dexterity over brute force.", 2, -1.2F);
        registerDagger("shadows_embrace", "Shadow's Embrace",
                "A dark-edge blade that disappears easily between cloak and heartbeat.", 3, -1.4F);
        registerDagger("mambas_kiss", "Mamba's Kiss",
                "A venomous-looking fang designed for evasive skirmishers.", 2, -1.2F);
        registerDagger("ghostly_sting", "Ghostly Sting",
                "Its pale edge slips in before most enemies even react.", 2, -1.3F);
        registerDagger("valkyries_thorn", "Valkyrie's Thorn",
                "A sacred thornblade carried by assassins who still honor the light.", 3, -1.4F);
        registerDagger("abyssal_fang", "Abyssal Fang",
                "A deep-sea edge with a weightier bite than most daggers dare carry.", 4, -1.7F);
        registerDagger("twilight_carver", "Twilight Carver",
                "A dusk-forged knife meant to end fights before dawn can witness them.", 3, -1.5F);
        registerDagger("oathbreaker", "Oathbreaker",
                "A grim ceremonial blade that hits harder than its slim shape suggests.", 4, -1.6F);
    }

    private static void registerOneHandedSwords() {
        registerSword("swords_1h", "ashen_rapier", "Ashen Rapier",
                "A lean dueling sword for fighters who win through tempo and reach.", 3, -2.1F);
        registerSword("swords_1h", "ironleaf_saber", "Ironleaf Saber",
                "A clean, dependable saber with a smooth draw and recovery.", 3, -2.2F);
        registerSword("swords_1h", "frostwake_blade", "Frostwake Blade",
                "A cold-edged sword that favors disciplined swordplay.", 4, -2.3F);
        registerSword("swords_1h", "warden_estoc", "Warden Estoc",
                "A long thrusting blade trusted by city guards and line breakers.", 4, -2.4F);
        registerSword("swords_1h", "crimson_talon", "Crimson Talon",
                "A fierce red blade made to pressure wounded prey.", 4, -2.2F);
        registerSword("swords_1h", "sunspire_blade", "Sunspire Blade",
                "A radiant sword with the poise of a knightly heirloom.", 5, -2.4F);
        registerSword("swords_1h", "cinder_brand", "Cinder Brand",
                "Its ember-lined edge encourages aggressive front-line trades.", 5, -2.3F);
        registerSword("swords_1h", "azure_fang", "Azure Fang",
                "A blue-forged blade with a lively, balanced swing.", 4, -2.1F);
        registerSword("swords_1h", "dawn_edge", "Dawn Edge",
                "A bright field sword meant for clean cuts and consistent pressure.", 4, -2.2F);
        registerSword("swords_1h", "verdant_blade", "Verdant Blade",
                "A green-tempered saber prized by wandering mercenaries.", 4, -2.0F);
        registerSword("swords_1h", "moonveil_saber", "Moonveil Saber",
                "A pale lunar blade that rewards accuracy and calm hands.", 5, -2.2F);
        registerSword("swords_1h", "abyss_needle", "Abyss Needle",
                "A dark needle-sword for duelists who trust a single perfect opening.", 5, -2.1F);
    }

    private static void registerOneHandedMaces() {
        registerMace("maces_1h", "ember_flail", "Ember Flail",
                "A burning headpiece that crushes armor with short explosive arcs.", 4, -2.7F);
        registerMace("maces_1h", "catacomb_breaker", "Catacomb Breaker",
                "A bone-dark mace favored in grim tomb raids.", 5, -2.8F);
        registerMace("maces_1h", "grave_blackjack", "Grave Blackjack",
                "A compact iron beater made for clerics who fight up close.", 4, -2.5F);
        registerMace("maces_1h", "meteor_star", "Meteor Star",
                "A spiked head that lands like a falling ember.", 5, -2.8F);
        registerMace("maces_1h", "hallowed_censer", "Hallowed Censer",
                "A sanctified weapon that blurs the line between relic and mace.", 4, -2.6F);
        registerMace("maces_1h", "sunburst_pernach", "Sunburst Pernach",
                "A bright war mace built to stagger shield walls.", 5, -2.7F);
        registerMace("maces_1h", "storm_morningstar", "Storm Morning Star",
                "An iron starhead that punishes anything caught in its path.", 6, -2.9F);
        registerMace("maces_1h", "pilgrim_maul", "Pilgrim Maul",
                "Simple in shape, brutal in impact, and easy to trust.", 5, -2.6F);
        registerMace("maces_1h", "frost_mallet", "Frost Mallet",
                "A cold-weight hammer that turns measured swings into crushing hits.", 5, -2.7F);
        registerMace("maces_1h", "obsidian_beater", "Obsidian Beater",
                "A dark crushing rod designed for stubborn armored foes.", 6, -2.8F);
        registerMace("maces_1h", "oracle_ram", "Oracle Ram",
                "An ornate priestly mace for battle chants and broken bones alike.", 5, -2.5F);
        registerMace("maces_1h", "gilded_comet", "Gilded Comet",
                "A decorated flanged mace that still fights like a true workhorse.", 6, -2.7F);
    }

    private static void registerWands() {
        registerWand("moonring_wand", "Moonring Wand",
                "A slender ring-tipped wand that channels spells with little wasted motion.", 1, -1.8F);
        registerWand("thunderbloom_wand", "Thunderbloom Wand",
                "Its blooming arc favors quick elemental bursts over raw force.", 1, -1.9F);
        registerWand("sageglass_wand", "Sageglass Wand",
                "A clear focus wand for apprentices who value stability.", 1, -1.8F);
        registerWand("suncoil_wand", "Suncoil Wand",
                "A warm golden conductor tuned for steady casting.", 2, -2.0F);
        registerWand("frostthorn_wand", "Frostthorn Wand",
                "A cold wand with a sharp crystal spine and excellent focus.", 1, -1.9F);
        registerWand("duskroot_wand", "Duskroot Wand",
                "A bent rootwood wand that hums under twilight mana.", 1, -1.8F);
        registerWand("gilded_crook", "Gilded Crook",
                "An ornate wand for magicians who like their tools refined.", 2, -1.9F);
        registerWand("arc_lattice_wand", "Arc Lattice Wand",
                "A wire-thin focus that excels at repeated short casts.", 1, -1.7F);
        registerWand("whisperstem_wand", "Whisperstem Wand",
                "A quiet branch-wand favored by careful elementalists.", 1, -1.8F);
        registerWand("ember_hook_wand", "Emberhook Wand",
                "A hooked wand that seems to catch sparks out of the air.", 2, -1.9F);
        registerWand("starlit_switch", "Starlit Switch",
                "A polished casting switch meant for agile support mages.", 1, -1.7F);
        registerWand("runebriar_wand", "Runebriar Wand",
                "A runed briar focus that sharpens the will behind each spell.", 2, -1.9F);
    }

    private static void registerStaves() {
        registerStaff("sunpriest_staff", "Sunpriest Staff",
                "A ceremonial staff that keeps prayer and spellwork aligned.", 2, -2.3F);
        registerStaff("verdant_crook", "Verdant Crook",
                "A living branch-staff with a calm, stable mana flow.", 2, -2.2F);
        registerStaff("tidebloom_staff", "Tidebloom Staff",
                "A coastal focus crowned with petals and soft blue current.", 2, -2.3F);
        registerStaff("embervine_staff", "Embervine Staff",
                "A fire-laced staff made for aggressive spell rotations.", 3, -2.4F);
        registerStaff("thornhalo_staff", "Thornhalo Staff",
                "Its crown of thorns grants authority to every cast.", 3, -2.5F);
        registerStaff("starseeker_staff", "Starseeker Staff",
                "A scholar's staff that rewards patient, deliberate casting.", 3, -2.4F);
        registerStaff("dreamwood_staff", "Dreamwood Staff",
                "A pale focus staff prized by wandering sages.", 2, -2.2F);
        registerStaff("magma_serpent_staff", "Magma Serpent Staff",
                "A twisting fire staff that carries dangerous heat.", 4, -2.6F);
        registerStaff("duskpetal_staff", "Duskpetal Staff",
                "A flowered staff that settles mana at the end of each cast.", 3, -2.3F);
        registerStaff("dawnmantle_staff", "Dawnmantle Staff",
                "A bright support staff fit for healers and battle priests.", 3, -2.4F);
        registerStaff("oracle_branch", "Oracle Branch",
                "A diviner's branch that turns calm focus into clear power.", 3, -2.2F);
        registerStaff("moonlily_staff", "Moonlily Staff",
                "A lunar staff that hums softly before each invocation.", 4, -2.5F);
    }

    private static void registerOneHandedWarAxes() {
        registerWarAxe("sunmaw_axe", "Sunmaw Axe",
                "A bright-bitted war axe that cuts clean and hard.", 5.0F, -3.0F);
        registerWarAxe("iron_harvester", "Iron Harvester",
                "A rough field axe reforged for battle.", 5.5F, -3.1F);
        registerWarAxe("ash_reaver", "Ash Reaver",
                "A soot-dark axe that leans into raw finishing power.", 6.0F, -3.1F);
        registerWarAxe("stormsplitter", "Stormsplitter",
                "A twin-edged axe that hits like a lightning crack.", 6.0F, -3.0F);
        registerWarAxe("gilded_reach", "Gilded Reach",
                "A long-hafted axe for disciplined merchants and guards.", 5.5F, -2.9F);
        registerWarAxe("wolfhook_axe", "Wolfhook Axe",
                "A hooked battle axe made to drag targets off balance.", 6.0F, -3.0F);
        registerWarAxe("scarlet_lopper", "Scarlet Lopper",
                "A brutal red axe favored by shock troops.", 6.5F, -3.1F);
        registerWarAxe("rune_chopper", "Rune Chopper",
                "Runed steel packed into a compact one-hand frame.", 5.5F, -2.9F);
        registerWarAxe("brass_cleaver", "Brass Cleaver",
                "A gold-lined cleaver head that still means business.", 5.0F, -2.8F);
        registerWarAxe("frost_crescent", "Frost Crescent",
                "A pale crescent axe with a keen, chilling bite.", 6.0F, -3.0F);
        registerWarAxe("ember_halberd_axe", "Ember Halberd Axe",
                "A pole-heavy axe adapted for brutal one-hand chops.", 6.5F, -3.2F);
        registerWarAxe("warden_biter", "Warden Biter",
                "A stubborn iron axe that shines in drawn-out fights.", 5.5F, -2.9F);
    }
}
