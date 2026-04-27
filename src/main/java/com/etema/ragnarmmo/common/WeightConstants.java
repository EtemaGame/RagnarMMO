package com.etema.ragnarmmo.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Shared constants for the weight/encumbrance system.
 * Used by both server-side logic (MerchantSkillEvents) and client-side UI
 * (StatsScreen).
 *
 * IMPORTANT: Keep these values synchronized. Any change here affects both
 * sides.
 * 
 * Weight is now calculated per-item (not per-stack), making inventory
 * management
 * a core gameplay mechanic similar to Ragnarok Online.
 */
public final class WeightConstants {

        private static final String MOD_ID = "ragnarmmo";

        private WeightConstants() {
        } // No instantiation

        private static TagKey<Item> itemTag(String path) {
                return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
        }

        // =============================
        // CAPACITY CONSTANTS
        // =============================

        /** Base carrying capacity (weight units) for all players */
        public static final double BASE_CAPACITY = 400.0D;

        /** Additional capacity per point of STR. Max STR=99 => +495 capacity */
        public static final double STR_CAPACITY_PER_POINT = 5.0D;

        /** Additional capacity per level of Cart Strength skill */
        public static final double CAPACITY_PER_CART_LEVEL = 100.0D;

        // =============================
        // ENCUMBRANCE PENALTY CONSTANTS
        // =============================

        /** Maximum speed penalty when overweight (85% = nearly immobile) */
        public static final double MAX_SPEED_PENALTY = 0.85D;

        /** Weight range over capacity before reaching max penalty */
        public static final double OVERWEIGHT_TO_MAX = 200.0D;

        // =============================
        // CART WEIGHT REDUCTION
        // =============================

        /** Weight reduction per level of Cart Strength (4% per level) */
        public static final double CART_WEIGHT_REDUCTION_PER_LEVEL = 0.04D;

        /** Maximum cart weight reduction cap (60%) */
        public static final double CART_WEIGHT_REDUCTION_CAP = 0.60D;

        // =============================
        // WEIGHT TAGS (22 total)
        // =============================

        // General Materials (1-9)
        public static final TagKey<Item> WEIGHT_EPHEMERAL = itemTag("weight_ephemeral");
        public static final TagKey<Item> WEIGHT_FEATHER = itemTag("weight_feather");
        public static final TagKey<Item> WEIGHT_LIGHT = itemTag("weight_light");
        public static final TagKey<Item> WEIGHT_COMMON = itemTag("weight_common");
        public static final TagKey<Item> WEIGHT_MEDIUM = itemTag("weight_medium");
        public static final TagKey<Item> WEIGHT_DENSE = itemTag("weight_dense");
        public static final TagKey<Item> WEIGHT_HEAVY = itemTag("weight_heavy");
        public static final TagKey<Item> WEIGHT_MASSIVE = itemTag("weight_massive");
        public static final TagKey<Item> WEIGHT_TITANIC = itemTag("weight_titanic");

        // Armor Materials (10-16)
        public static final TagKey<Item> WEIGHT_ARMOR_LEATHER = itemTag("weight_armor_leather");
        public static final TagKey<Item> WEIGHT_ARMOR_TURTLE = itemTag("weight_armor_turtle");
        public static final TagKey<Item> WEIGHT_ARMOR_CHAIN = itemTag("weight_armor_chain");
        public static final TagKey<Item> WEIGHT_ARMOR_GOLD = itemTag("weight_armor_gold");
        public static final TagKey<Item> WEIGHT_ARMOR_IRON = itemTag("weight_armor_iron");
        public static final TagKey<Item> WEIGHT_ARMOR_DIAMOND = itemTag("weight_armor_diamond");
        public static final TagKey<Item> WEIGHT_ARMOR_NETHERITE = itemTag("weight_armor_netherite");

        // Tool/Weapon Materials (17-22)
        public static final TagKey<Item> WEIGHT_TOOL_WOOD = itemTag("weight_tool_wood");
        public static final TagKey<Item> WEIGHT_TOOL_STONE = itemTag("weight_tool_stone");
        public static final TagKey<Item> WEIGHT_TOOL_GOLD = itemTag("weight_tool_gold");
        public static final TagKey<Item> WEIGHT_TOOL_IRON = itemTag("weight_tool_iron");
        public static final TagKey<Item> WEIGHT_TOOL_DIAMOND = itemTag("weight_tool_diamond");
        public static final TagKey<Item> WEIGHT_TOOL_NETHERITE = itemTag("weight_tool_netherite");

        // =============================
        // GENERAL MATERIAL WEIGHT VALUES
        // =============================

        public static final double WEIGHT_VAL_EPHEMERAL = 0.01D;
        public static final double WEIGHT_VAL_FEATHER = 0.05D;
        public static final double WEIGHT_VAL_LIGHT = 0.1D;
        public static final double WEIGHT_VAL_COMMON = 0.25D;
        public static final double WEIGHT_VAL_MEDIUM = 0.4D;
        public static final double WEIGHT_VAL_DENSE = 0.8D;
        public static final double WEIGHT_VAL_HEAVY = 1.5D;
        public static final double WEIGHT_VAL_MASSIVE = 3.0D;
        public static final double WEIGHT_VAL_TITANIC = 10.0D;

        // =============================
        // ARMOR MATERIAL WEIGHT VALUES
        // =============================

        public static final double WEIGHT_VAL_ARMOR_LEATHER = 15.0D;
        public static final double WEIGHT_VAL_ARMOR_TURTLE = 20.0D;
        public static final double WEIGHT_VAL_ARMOR_CHAIN = 25.0D;
        public static final double WEIGHT_VAL_ARMOR_DIAMOND = 30.0D;
        public static final double WEIGHT_VAL_ARMOR_IRON = 35.0D;
        public static final double WEIGHT_VAL_ARMOR_GOLD = 45.0D;
        public static final double WEIGHT_VAL_ARMOR_NETHERITE = 60.0D;

        // =============================
        // TOOL/WEAPON MATERIAL WEIGHT VALUES
        // =============================

        public static final double WEIGHT_VAL_TOOL_WOOD = 10.0D;
        public static final double WEIGHT_VAL_TOOL_DIAMOND = 15.0D;
        public static final double WEIGHT_VAL_TOOL_STONE = 20.0D;
        public static final double WEIGHT_VAL_TOOL_IRON = 25.0D;
        public static final double WEIGHT_VAL_TOOL_GOLD = 40.0D;
        public static final double WEIGHT_VAL_TOOL_NETHERITE = 50.0D;

        // =============================
        // SPECIAL ITEM WEIGHTS
        // =============================

        /** Shield weight */
        public static final double WEIGHT_SHIELD = 30.0D;

        /** Elytra weight */
        public static final double WEIGHT_ELYTRA = 10.0D;

        /** Bow weight */
        public static final double WEIGHT_BOW = 12.0D;

        /** Crossbow weight */
        public static final double WEIGHT_CROSSBOW = 18.0D;

        /** Trident weight */
        public static final double WEIGHT_TRIDENT = 35.0D;

        // =============================
        // DEFAULT WEIGHTS
        // =============================

        /** Default weight for stackable items without a tag (blocks, etc.) */
        public static final double WEIGHT_DEFAULT_STACKABLE = 0.25D;

        /** Default weight for non-stackable items without a tag */
        public static final double WEIGHT_DEFAULT_UNSTACKABLE = 0.4D;

        /** Default weight for unknown modded tiered items */
        public static final double WEIGHT_DEFAULT_TIERED = 20.0D;

        /** Default weight for unknown modded armor */
        public static final double WEIGHT_DEFAULT_ARMOR = 30.0D;
}
