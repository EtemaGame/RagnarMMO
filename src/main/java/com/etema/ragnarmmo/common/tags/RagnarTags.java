package com.etema.ragnarmmo.common.tags;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Registry class for all custom Tags used by RagnarMMO.
 * Follows NeoForge Data-Driven conventions for Entities and Items.
 */
public class RagnarTags {

    public static class Entities {
        // === Races ===
        public static final TagKey<EntityType<?>> RACE_DEMIHUMAN = create("races/demihuman");
        public static final TagKey<EntityType<?>> RACE_BRUTE = create("races/brute");
        public static final TagKey<EntityType<?>> RACE_INSECT = create("races/insect");
        public static final TagKey<EntityType<?>> RACE_FISH = create("races/fish");
        public static final TagKey<EntityType<?>> RACE_DEMON = create("races/demon");
        public static final TagKey<EntityType<?>> RACE_UNDEAD = create("races/undead");
        public static final TagKey<EntityType<?>> RACE_FORMLESS = create("races/formless");
        public static final TagKey<EntityType<?>> RACE_PLANT = create("races/plant");
        public static final TagKey<EntityType<?>> RACE_ANGEL = create("races/angel");
        public static final TagKey<EntityType<?>> RACE_DRAGON = create("races/dragon");

        // === Sizes ===
        public static final TagKey<EntityType<?>> SIZE_SMALL = create("sizes/small");
        public static final TagKey<EntityType<?>> SIZE_MEDIUM = create("sizes/medium");
        public static final TagKey<EntityType<?>> SIZE_LARGE = create("sizes/large");

        // === Elements (Defending) ===
        public static final TagKey<EntityType<?>> ELEMENT_NEUTRAL = create("elements/neutral");
        public static final TagKey<EntityType<?>> ELEMENT_WATER = create("elements/water");
        public static final TagKey<EntityType<?>> ELEMENT_EARTH = create("elements/earth");
        public static final TagKey<EntityType<?>> ELEMENT_FIRE = create("elements/fire");
        public static final TagKey<EntityType<?>> ELEMENT_WIND = create("elements/wind");
        public static final TagKey<EntityType<?>> ELEMENT_POISON = create("elements/poison");
        public static final TagKey<EntityType<?>> ELEMENT_SCARLET = create("elements/holy"); // Holy is scarlet in RO
                                                                                             // engine
        public static final TagKey<EntityType<?>> ELEMENT_DARK = create("elements/dark");
        public static final TagKey<EntityType<?>> ELEMENT_GHOST = create("elements/ghost");
        public static final TagKey<EntityType<?>> ELEMENT_UNDEAD = create("elements/undead");

        // Helper
        private static TagKey<EntityType<?>> create(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(RagnarMMO.MODID, name));
        }
    }

    public static class Items {
        // === Elements (Offending / Weapon) ===
        public static final TagKey<Item> ELEMENT_NEUTRAL = create("elements/neutral");
        public static final TagKey<Item> ELEMENT_WATER = create("elements/water");
        public static final TagKey<Item> ELEMENT_EARTH = create("elements/earth");
        public static final TagKey<Item> ELEMENT_FIRE = create("elements/fire");
        public static final TagKey<Item> ELEMENT_WIND = create("elements/wind");
        public static final TagKey<Item> ELEMENT_POISON = create("elements/poison");
        public static final TagKey<Item> ELEMENT_HOLY = create("elements/holy");
        public static final TagKey<Item> ELEMENT_DARK = create("elements/dark");
        public static final TagKey<Item> ELEMENT_GHOST = create("elements/ghost");

        // Helper
        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(RagnarMMO.MODID, name));
        }
    }
}
