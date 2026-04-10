package com.etema.ragnarmmo.system.mobstats.config;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.List;

public final class MobConfig {

        public enum LevelScalingMode {
                PLAYER_LEVEL,
                DISTANCE,
                BIOME_DISTANCE
        }

        public static final ForgeConfigSpec SPEC;

        // ==========================================
        // STATS
        // ==========================================
        public static final ForgeConfigSpec.IntValue MAX_LEVEL;
        public static final ForgeConfigSpec.DoubleValue ELITE_CHANCE;
        public static final ForgeConfigSpec.DoubleValue MINI_BOSS_CHANCE;
        public static final ForgeConfigSpec.DoubleValue BOSS_CHANCE;
        public static final ForgeConfigSpec.DoubleValue MVP_CHANCE;
        public static final ForgeConfigSpec.DoubleValue NATURAL_TIER_CHANCE_SCALE;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_EXCLUDE_LIST;

        // Attributes (Base & Calculations)
        public static final ForgeConfigSpec.DoubleValue HP_BASE;
        public static final ForgeConfigSpec.DoubleValue VIT_TO_HP;
        public static final ForgeConfigSpec.DoubleValue LEVEL_TO_HP;
        public static final ForgeConfigSpec.DoubleValue ATK_BASE;
        public static final ForgeConfigSpec.DoubleValue STR_TO_ATK;
        public static final ForgeConfigSpec.DoubleValue DEX_TO_ATK;
        public static final ForgeConfigSpec.DoubleValue LUK_TO_ATK;
        public static final ForgeConfigSpec.DoubleValue ARMOR_BASE;
        public static final ForgeConfigSpec.DoubleValue VIT_TO_ARMOR;
        public static final ForgeConfigSpec.DoubleValue INT_TO_ARMOR;
        public static final ForgeConfigSpec.DoubleValue AGI_TO_SPEED;
        public static final ForgeConfigSpec.DoubleValue MAX_MOVEMENT_SPEED;
        public static final ForgeConfigSpec.DoubleValue LUK_TO_KB_RESIST;

        // Combat
        public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_STR_POINT;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_DEX_POINT;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_PER_VIT_POINT;

        // Progression (Points)
        public static final ForgeConfigSpec.IntValue BASE_POINTS_NORMAL;
        public static final ForgeConfigSpec.IntValue BASE_POINTS_ELITE;
        public static final ForgeConfigSpec.IntValue BASE_POINTS_MINI_BOSS;
        public static final ForgeConfigSpec.IntValue BASE_POINTS_BOSS;
        public static final ForgeConfigSpec.IntValue BASE_POINTS_MVP;
        public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL_NORMAL;
        public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL_ELITE;
        public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL_MINI_BOSS;
        public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL_BOSS;
        public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL_MVP;

        // Multipliers
        public static final ForgeConfigSpec.DoubleValue HEALTH_MULT_NORMAL;
        public static final ForgeConfigSpec.DoubleValue HEALTH_MULT_ELITE;
        public static final ForgeConfigSpec.DoubleValue HEALTH_MULT_MINI_BOSS;
        public static final ForgeConfigSpec.DoubleValue HEALTH_MULT_BOSS;
        public static final ForgeConfigSpec.DoubleValue HEALTH_MULT_MVP;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT_NORMAL;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT_ELITE;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT_MINI_BOSS;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT_BOSS;
        public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT_MVP;
        public static final ForgeConfigSpec.DoubleValue DEFENSE_MULT_NORMAL;
        public static final ForgeConfigSpec.DoubleValue DEFENSE_MULT_ELITE;
        public static final ForgeConfigSpec.DoubleValue DEFENSE_MULT_MINI_BOSS;
        public static final ForgeConfigSpec.DoubleValue DEFENSE_MULT_BOSS;
        public static final ForgeConfigSpec.DoubleValue DEFENSE_MULT_MVP;

        // ==========================================
        // WORLD SCALING (Zone)
        // ==========================================
        public static final ForgeConfigSpec.BooleanValue ENABLE_BIOME_AUTO_CLASSIFY;
        public static final ForgeConfigSpec.BooleanValue RENDER_NUMERIC_HEALTH;
        public static final ForgeConfigSpec.EnumValue<LevelScalingMode> LEVEL_SCALING_MODE;
        public static final ForgeConfigSpec.IntValue PLAYER_LEVEL_RADIUS;
        public static final ForgeConfigSpec.IntValue PLAYER_LEVEL_VARIANCE;
        public static final DimensionConfig OVERWORLD;
        public static final DimensionConfig NETHER;
        public static final DimensionConfig END;

        // Global Minimum Rules
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_MIN_LEVELS;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_MIN_LEVELS;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_MIN_LEVELS;



        // ==========================================
        // MULTIPLAYER & RARITY
        // ==========================================
        // Multiplayer Scaling
        public static final ForgeConfigSpec.DoubleValue PARTY_SCALING_RADIUS;
        public static final ForgeConfigSpec.DoubleValue PARTY_HP_MULTIPLIER;
        public static final ForgeConfigSpec.DoubleValue PARTY_ATK_MULTIPLIER;



        public static class DimensionConfig {
                public final ForgeConfigSpec.IntValue RING_SIZE;
                public final ForgeConfigSpec.DoubleValue DISTANCE_MULTIPLIER;
                public final ForgeConfigSpec.IntValue RINGS_CAP;
                public final ForgeConfigSpec.IntValue MIN_FLOOR;
                public final ForgeConfigSpec.IntValue MAX_CAP;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> TIERS;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOME_TO_TIER;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> DISTANCE_BANDS;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOME_DISTANCE_BANDS;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> DEPTH_RULES;
                public final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_PROXIMITY;

                public DimensionConfig(ForgeConfigSpec.Builder b, String dimName, int ringSize, double distMult,
                                int minFloor,
                                int maxCap,
                                java.util.function.Supplier<List<String>> defaultBiomes,
                                java.util.function.Supplier<List<String>> defaultDistanceBands,
                                java.util.function.Supplier<List<String>> defaultBiomeDistanceBands) {
                        b.push(dimName);
                        RING_SIZE = b.comment("Blocks from (0,0) per ring").defineInRange("ring_size", ringSize, 100,
                                        100000);
                        DISTANCE_MULTIPLIER = b.comment("Factor = mult ^ rings").defineInRange("distance_multiplier",
                                        distMult, 1.0, 10.0);
                        RINGS_CAP = b.defineInRange("rings_cap", 50, 1, 1000);
                        MIN_FLOOR = b.defineInRange("min_floor", minFloor, 1, 100000);
                        MAX_CAP = b.defineInRange("max_cap", maxCap, 1, 100000);
                        TIERS = b.comment("tier=min-max").defineList("tiers", List.of("very_easy=1-4", "easy=1-5",
                                        "medium=3-8", "hard=6-12", "very_hard=10-18"), o -> o instanceof String);
                        BIOME_TO_TIER = b.comment("biome=tier").defineList("biome_to_tier", defaultBiomes.get(),
                                        o -> o instanceof String);
                        DISTANCE_BANDS = b.comment("distance range to level range, format: 0-999=1-5 or 2500+=10-15")
                                        .defineList("distance_bands", defaultDistanceBands.get(), o -> o instanceof String);
                        BIOME_DISTANCE_BANDS = b.comment(
                                        "biome + distance to level range, format: minecraft:plains|0-999=1-5")
                                        .defineList("biome_distance_bands", defaultBiomeDistanceBands.get(),
                                                        o -> o instanceof String);
                        DEPTH_RULES = b.comment("sky_visible/y<0=factor").defineList("depth_rules",
                                        List.of("sky_visible=1.0", "no_sky_visible=1.15", "y<0=1.25", "y<-32=1.35"),
                                        o -> o instanceof String);
                        STRUCTURE_PROXIMITY = b.comment("structure=radius,factor").defineList("structure_proximity",
                                        Collections.emptyList(), o -> o instanceof String);
                        b.pop();
                }
        }

        static {
                ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

                // 1. STATS
                b.comment("Core mob statistics and scaling").push("stats");

                MAX_LEVEL = b.defineInRange("max_level", 0, 0, 100000);
                ELITE_CHANCE = b.defineInRange("elite_chance", 0.08D, 0.0D, 1.0D);
                MINI_BOSS_CHANCE = b.defineInRange("mini_boss_chance", 0.02D, 0.0D, 1.0D);
                BOSS_CHANCE = b.defineInRange("boss_chance", 0.01D, 0.0D, 1.0D);
                MVP_CHANCE = b.defineInRange("mvp_chance", 0.002D, 0.0D, 1.0D);
                NATURAL_TIER_CHANCE_SCALE = b.comment("Global multiplier applied to natural elite/boss rolls")
                                .defineInRange("natural_tier_chance_scale", 0.2D, 0.0D, 1.0D);
                MOB_EXCLUDE_LIST = b.defineList("mob_exclude_list",
                                List.of("minecraft:armor_stand", "minecraft:villager"), o -> o instanceof String);

                b.push("attributes");
                HP_BASE = b.defineInRange("hp_base", 20.0D, 0.0D, 1.0E9D);
                VIT_TO_HP = b.defineInRange("vit_to_hp", 5.0D, 0.0D, 1.0E6D);
                LEVEL_TO_HP = b.defineInRange("level_to_hp", 5.0D, 0.0D, 1.0E6D);
                ATK_BASE = b.defineInRange("atk_base", 4.0D, 0.0D, 1.0E9D);
                STR_TO_ATK = b.defineInRange("str_to_atk", 1.0D, 0.0D, 1.0E6D);
                DEX_TO_ATK = b.defineInRange("dex_to_atk", 0.2D, 0.0D, 1.0E6D);
                LUK_TO_ATK = b.defineInRange("luk_to_atk", 0.1D, 0.0D, 1.0E6D);
                ARMOR_BASE = b.defineInRange("armor_base", 0.0D, 0.0D, 1.0E9D);
                VIT_TO_ARMOR = b.defineInRange("vit_to_armor", 0.2D, 0.0D, 1.0E6D);
                INT_TO_ARMOR = b.defineInRange("int_to_armor", 0.1D, 0.0D, 1.0E6D);
                AGI_TO_SPEED = b.defineInRange("agi_to_speed", 0.0025D, 0.0D, 1.0D);
                MAX_MOVEMENT_SPEED = b.comment("Hard cap for final mob movement speed after scaling")
                                .defineInRange("max_movement_speed", 0.36D, 0.05D, 2.0D);
                LUK_TO_KB_RESIST = b.defineInRange("luk_to_kb_resist", 0.002D, 0.0D, 1.0D);
                b.pop(); // attributes

                b.push("combat");
                DAMAGE_PER_STR_POINT = b.defineInRange("damage_per_str", 0.01D, 0.0D, 100.0D);
                DAMAGE_PER_DEX_POINT = b.defineInRange("damage_per_dex", 0.005D, 0.0D, 100.0D);
                DAMAGE_REDUCTION_PER_VIT_POINT = b.defineInRange("reduction_per_vit", 0.003D, 0.0D, 1.0D);
                b.pop(); // combat

                b.push("progression");
                BASE_POINTS_NORMAL = b.defineInRange("base_points.normal", 5, 0, 100000);
                BASE_POINTS_ELITE = b.defineInRange("base_points.elite", 20, 0, 100000);
                BASE_POINTS_MINI_BOSS = b.defineInRange("base_points.mini_boss", 30, 0, 100000);
                BASE_POINTS_BOSS = b.defineInRange("base_points.boss", 40, 0, 100000);
                BASE_POINTS_MVP = b.defineInRange("base_points.mvp", 60, 0, 100000);
                POINTS_PER_LEVEL_NORMAL = b.defineInRange("per_level.normal", 3, 0, 10000);
                POINTS_PER_LEVEL_ELITE = b.defineInRange("per_level.elite", 5, 0, 10000);
                POINTS_PER_LEVEL_MINI_BOSS = b.defineInRange("per_level.mini_boss", 6, 0, 10000);
                POINTS_PER_LEVEL_BOSS = b.defineInRange("per_level.boss", 8, 0, 10000);
                POINTS_PER_LEVEL_MVP = b.defineInRange("per_level.mvp", 10, 0, 10000);
                b.pop(); // progression

                b.push("multipliers");
                HEALTH_MULT_NORMAL = b.defineInRange("health.normal", 1.0D, 0.0D, 100.0D);
                HEALTH_MULT_ELITE = b.defineInRange("health.elite", 2.0D, 0.0D, 100.0D);
                HEALTH_MULT_MINI_BOSS = b.defineInRange("health.mini_boss", 3.0D, 0.0D, 100.0D);
                HEALTH_MULT_BOSS = b.defineInRange("health.boss", 4.0D, 0.0D, 100.0D);
                HEALTH_MULT_MVP = b.defineInRange("health.mvp", 6.0D, 0.0D, 100.0D);
                DAMAGE_MULT_NORMAL = b.defineInRange("damage.normal", 1.0D, 0.0D, 100.0D);
                DAMAGE_MULT_ELITE = b.defineInRange("damage.elite", 1.5D, 0.0D, 100.0D);
                DAMAGE_MULT_MINI_BOSS = b.defineInRange("damage.mini_boss", 2.0D, 0.0D, 100.0D);
                DAMAGE_MULT_BOSS = b.defineInRange("damage.boss", 2.5D, 0.0D, 100.0D);
                DAMAGE_MULT_MVP = b.defineInRange("damage.mvp", 3.2D, 0.0D, 100.0D);
                DEFENSE_MULT_NORMAL = b.defineInRange("defense.normal", 1.0D, 0.0D, 100.0D);
                DEFENSE_MULT_ELITE = b.defineInRange("defense.elite", 1.5D, 0.0D, 100.0D);
                DEFENSE_MULT_MINI_BOSS = b.defineInRange("defense.mini_boss", 1.65D, 0.0D, 100.0D);
                DEFENSE_MULT_BOSS = b.defineInRange("defense.boss", 1.8D, 0.0D, 100.0D);
                DEFENSE_MULT_MVP = b.defineInRange("defense.mvp", 2.2D, 0.0D, 100.0D);
                b.pop(); // multipliers

                b.pop(); // Stats

                // 2. WORLD SCALING
                b.comment("Level scaling based on distance, dimensions, and structures").push("world_scaling");
                ENABLE_BIOME_AUTO_CLASSIFY = b.comment("Automatically classify modded biomes based on tags and stats")
                                .define("auto_classify_biomes", true);
                RENDER_NUMERIC_HEALTH = b.comment("Show numeric health (Current / Max) on mob health bar")
                                .define("render_numeric_health", true);
                LEVEL_SCALING_MODE = b.comment(
                                "Automatic selector for non-manual hostile mob levels: PLAYER_LEVEL, DISTANCE, BIOME_DISTANCE",
                                "The datapack MANUAL path bypasses this selector.")
                                .defineEnum("level_scaling_mode", LevelScalingMode.DISTANCE);
                PLAYER_LEVEL_RADIUS = b.comment("Radius used when anchoring hostile mob level to nearby players")
                                .defineInRange("player_level_radius", 64, 8, 256);
                PLAYER_LEVEL_VARIANCE = b.comment("Random variance applied around player-level scaling")
                                .defineInRange("player_level_variance", 2, 0, 100);

                OVERWORLD = new DimensionConfig(b, "overworld", 1000, 1.35, 1, 160, () -> List.of(
                                "minecraft:plains=easy",
                                "minecraft:sunflower_plains=easy",
                                "minecraft:snowy_plains=easy",
                                "minecraft:ice_spikes=hard",
                                "minecraft:desert=medium",
                                "minecraft:swamp=medium",
                                "minecraft:mangrove_swamp=medium",
                                "minecraft:forest=easy",
                                "minecraft:flower_forest=easy",
                                "minecraft:birch_forest=easy",
                                "minecraft:dark_forest=hard",
                                "minecraft:old_growth_birch_forest=medium",
                                "minecraft:old_growth_pine_taiga=medium",
                                "minecraft:old_growth_spruce_taiga=medium",
                                "minecraft:taiga=medium",
                                "minecraft:snowy_taiga=medium",
                                "minecraft:savanna=medium",
                                "minecraft:savanna_plateau=medium",
                                "minecraft:windswept_hills=hard",
                                "minecraft:windswept_gravelly_hills=hard",
                                "minecraft:windswept_forest=hard",
                                "minecraft:windswept_savanna=hard",
                                "minecraft:jungle=hard",
                                "minecraft:sparse_jungle=medium",
                                "minecraft:bamboo_jungle=very_hard",
                                "minecraft:badlands=hard",
                                "minecraft:eroded_badlands=hard",
                                "minecraft:wooded_badlands=hard",
                                "minecraft:meadow=easy",
                                "minecraft:grove=medium",
                                "minecraft:snowy_slopes=hard",
                                "minecraft:frozen_peaks=very_hard",
                                "minecraft:jagged_peaks=very_hard",
                                "minecraft:stony_peaks=very_hard",
                                "minecraft:river=easy",
                                "minecraft:frozen_river=easy",
                                "minecraft:beach=easy",
                                "minecraft:snowy_beach=easy",
                                "minecraft:stony_shore=medium",
                                "minecraft:deep_dark=very_hard",
                                "minecraft:dripstone_caves=hard",
                                "minecraft:lush_caves=medium",
                                "minecraft:mushroom_fields=easy", // Rare but safe
                                "minecraft:ocean=medium",
                                "minecraft:deep_ocean=hard",
                                "minecraft:warm_ocean=medium",
                                "minecraft:lukewarm_ocean=medium",
                                "minecraft:deep_lukewarm_ocean=hard",
                                "minecraft:cold_ocean=medium",
                                "minecraft:deep_cold_ocean=hard",
                                "minecraft:frozen_ocean=medium",
                                "minecraft:deep_frozen_ocean=hard"),
                                () -> List.of(
                                                "0-999=1-5",
                                                "1000-2499=5-10",
                                                "2500-4999=10-15",
                                                "5000-8999=15-22",
                                                "9000-14999=22-30",
                                                "15000-24999=30-40",
                                                "25000+=40-55"),
                                () -> List.of(
                                                "minecraft:plains|0-999=1-5",
                                                "minecraft:plains|1000-2499=5-10",
                                                "minecraft:plains|2500+=10-15",
                                                "minecraft:desert|0-999=4-9",
                                                "minecraft:desert|1000-2499=9-14",
                                                "minecraft:desert|2500+=14-20",
                                                "minecraft:jungle|0-999=8-12",
                                                "minecraft:jungle|1000-2499=12-18",
                                                "minecraft:jungle|2500+=18-26",
                                                "minecraft:deep_dark|0+=30-45"));

                NETHER = new DimensionConfig(b, "nether", 500, 1.25, 30, 320, () -> List.of(
                                "minecraft:nether_wastes=medium",
                                "minecraft:soul_sand_valley=hard",
                                "minecraft:crimson_forest=medium",
                                "minecraft:warped_forest=medium",
                                "minecraft:basalt_deltas=very_hard"),
                                () -> List.of(
                                                "0-999=30-38",
                                                "1000-2499=38-48",
                                                "2500-4999=48-58",
                                                "5000-8999=58-70",
                                                "9000+=70-90"),
                                () -> List.of(
                                                "minecraft:nether_wastes|0-999=30-36",
                                                "minecraft:nether_wastes|1000+=36-45",
                                                "minecraft:soul_sand_valley|0-999=38-46",
                                                "minecraft:soul_sand_valley|1000+=46-58",
                                                "minecraft:basalt_deltas|0-999=45-55",
                                                "minecraft:basalt_deltas|1000+=55-70"));

                END = new DimensionConfig(b, "end", 1000, 1.22, 60, 420, () -> List.of(
                                "minecraft:the_end=medium",
                                "minecraft:small_end_islands=medium",
                                "minecraft:end_midlands=medium",
                                "minecraft:end_highlands=hard",
                                "minecraft:end_barrens=hard"),
                                () -> List.of(
                                                "0-999=60-70",
                                                "1000-2499=70-82",
                                                "2500-4999=82-96",
                                                "5000+=96-115"),
                                () -> List.of(
                                                "minecraft:the_end|0-999=60-68",
                                                "minecraft:the_end|1000+=68-78",
                                                "minecraft:end_highlands|0-999=70-82",
                                                "minecraft:end_highlands|1000+=82-96",
                                                "minecraft:small_end_islands|0-999=62-72",
                                                "minecraft:small_end_islands|1000+=72-85"));

                b.push("minimums");
                DIMENSION_MIN_LEVELS = b.comment("id=level").defineList("dimension_min_levels", List.of(),
                                o -> o instanceof String);
                STRUCTURE_MIN_LEVELS = b.comment("id=level").defineList("structure_min_levels", List.of(),
                                o -> o instanceof String);
                BOSS_MIN_LEVELS = b.comment("id=level").defineList("boss_min_levels", List.of(),
                                o -> o instanceof String);
                b.pop(); // minimums
                b.pop(); // World scaling



                b.push("multiplayer_scaling");
                PARTY_SCALING_RADIUS = b
                                .comment("Radius to check for nearby players for difficulty scaling")
                                .defineInRange("radius", 32.0, 1.0, 256.0);
                PARTY_HP_MULTIPLIER = b
                                .comment("HP multiplier per extra player nearby")
                                .defineInRange("hp_per_player", 0.4, 0.0, 10.0);
                PARTY_ATK_MULTIPLIER = b
                                .comment("ATK multiplier per extra player nearby")
                                .defineInRange("atk_per_player", 0.2, 0.0, 10.0);
                b.pop();

                SPEC = b.build();
        }

        private MobConfig() {
        }

        /**
         * Legacy {@link MobTier}-based progression and multiplier helpers for the old mob-stats
         * pipeline.
         *
         * <p>These helpers remain compatibility utilities. New migration work should prefer the
         * semantic mob models and read surfaces instead of treating {@link MobTier} as the primary
         * encounter model.</p>
         */
        public static int basePoints(MobTier tier) {
                return switch (tier) {
                        case NORMAL -> BASE_POINTS_NORMAL.get();
                        case ELITE -> BASE_POINTS_ELITE.get();
                        case MINI_BOSS -> BASE_POINTS_MINI_BOSS.get();
                        case BOSS -> BASE_POINTS_BOSS.get();
                        case MVP -> BASE_POINTS_MVP.get();
                };
        }

        public static int pointsPerLevel(MobTier tier) {
                return switch (tier) {
                        case NORMAL -> POINTS_PER_LEVEL_NORMAL.get();
                        case ELITE -> POINTS_PER_LEVEL_ELITE.get();
                        case MINI_BOSS -> POINTS_PER_LEVEL_MINI_BOSS.get();
                        case BOSS -> POINTS_PER_LEVEL_BOSS.get();
                        case MVP -> POINTS_PER_LEVEL_MVP.get();
                };
        }

        public static double healthMultiplier(MobTier tier) {
                return switch (tier) {
                        case NORMAL -> HEALTH_MULT_NORMAL.get();
                        case ELITE -> HEALTH_MULT_ELITE.get();
                        case MINI_BOSS -> HEALTH_MULT_MINI_BOSS.get();
                        case BOSS -> HEALTH_MULT_BOSS.get();
                        case MVP -> HEALTH_MULT_MVP.get();
                };
        }

        public static double damageMultiplier(MobTier tier) {
                return switch (tier) {
                        case NORMAL -> DAMAGE_MULT_NORMAL.get();
                        case ELITE -> DAMAGE_MULT_ELITE.get();
                        case MINI_BOSS -> DAMAGE_MULT_MINI_BOSS.get();
                        case BOSS -> DAMAGE_MULT_BOSS.get();
                        case MVP -> DAMAGE_MULT_MVP.get();
                };
        }

        public static double defenseMultiplier(MobTier tier) {
                return switch (tier) {
                        case NORMAL -> DEFENSE_MULT_NORMAL.get();
                        case ELITE -> DEFENSE_MULT_ELITE.get();
                        case MINI_BOSS -> DEFENSE_MULT_MINI_BOSS.get();
                        case BOSS -> DEFENSE_MULT_BOSS.get();
                        case MVP -> DEFENSE_MULT_MVP.get();
                };
        }
}
