package com.etema.ragnarmmo.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * Unified configuration for RagnarMMO.
 * Only 2 files: ragnarmmo-client.toml (HUD) and ragnarmmo-server.toml (all server rules).
 */
public final class RagnarConfigs {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final Server SERVER;

    static {
        Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();

        Pair<Server, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    private RagnarConfigs() {}

    // ========================================================================
    // CLIENT CONFIG
    // ========================================================================
    public static final class Client {
        public final Hud hud;
        public final PartyHud partyHud;

        Client(ForgeConfigSpec.Builder builder) {
            this.hud = new Hud(builder);
            this.partyHud = new PartyHud(builder);
        }

        public enum HudAnchor {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
        }

        public static final class Hud {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.DoubleValue scale;
            public final ForgeConfigSpec.IntValue width;
            public final ForgeConfigSpec.IntValue backgroundAlpha;
            public final ForgeConfigSpec.BooleanValue replaceVanillaSurvivalBars;

            public final HudComponent status;
            public final HudComponent cast;
            public final HudComponent skillHotbar;
            public final HudComponent partyFrame;
            public final HudComponent targetFrame;
            public final HudComponent notifications;

            Hud(ForgeConfigSpec.Builder builder) {
                builder.comment("HUD overlay configuration").push("hud");
                enabled = builder.comment("Show the experience HUD overlay").define("enabled", true);
                scale = builder.comment("Scale of the HUD overlay").defineInRange("scale", 1.0, 0.5, 3.0);
                width = builder.comment("Width of the HUD panel in pixels").defineInRange("width", 150, 120, 400);
                backgroundAlpha = builder.comment("Background opacity (0-255)").defineInRange("background_alpha", 0, 0, 255);
                replaceVanillaSurvivalBars = builder.comment("If true, hide vanilla health/food bars").define("replace_vanilla_survival_bars", true);

                status = new HudComponent(builder, "status", 0.0, 0.0, 10);
                cast = new HudComponent(builder, "cast", 0.5, 0.65, 40);
                skillHotbar = new HudComponent(builder, "skill_hotbar", 0.5, 1.0, 30);
                partyFrame = new HudComponent(builder, "party_frame", 0.0, 0.12, 20);
                targetFrame = new HudComponent(builder, "target_frame", 0.5, 0.08, 25);
                notifications = new HudComponent(builder, "notifications", 1.0, 0.12, 50);
                builder.pop();
            }

            public static class HudComponent {
                public final ForgeConfigSpec.DoubleValue anchorX;
                public final ForgeConfigSpec.DoubleValue anchorY;
                public final ForgeConfigSpec.BooleanValue enabled;
                public final ForgeConfigSpec.DoubleValue scale;
                public final ForgeConfigSpec.IntValue backgroundAlpha;
                public final ForgeConfigSpec.BooleanValue showBackground;
                public final ForgeConfigSpec.IntValue zOrder;

                public HudComponent(ForgeConfigSpec.Builder builder, String name, double x, double y, int z) {
                    builder.push(name);
                    anchorX = builder.defineInRange("anchor_x", x, 0.0, 1.0);
                    anchorY = builder.defineInRange("anchor_y", y, 0.0, 1.0);
                    enabled = builder.define("enabled", true);
                    scale = builder.defineInRange("scale", 1.0, 0.1, 5.0);
                    backgroundAlpha = builder.defineInRange("background_alpha", 100, 0, 255);
                    showBackground = builder.define("show_background", true);
                    zOrder = builder.defineInRange("z_order", z, -1000, 1000);
                    builder.pop();
                }
            }
        }

        public static final class PartyHud {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.DoubleValue scale;
            public final ForgeConfigSpec.EnumValue<HudAnchor> anchor;
            public final ForgeConfigSpec.IntValue xOffset;
            public final ForgeConfigSpec.IntValue yOffset;
            public final ForgeConfigSpec.BooleanValue showSelf;

            PartyHud(ForgeConfigSpec.Builder builder) {
                builder.comment("Party HUD configuration").push("party_hud");
                enabled = builder.define("enabled", true);
                scale = builder.defineInRange("scale", 1.0, 0.75, 1.5);
                anchor = builder.defineEnum("anchor", HudAnchor.TOP_LEFT);
                xOffset = builder.defineInRange("x_offset", 10, -1000, 1000);
                yOffset = builder.defineInRange("y_offset", 30, -1000, 1000);
                showSelf = builder.define("show_self", false);
                builder.pop();
            }
        }
    }

    // ========================================================================
    // SERVER CONFIG
    // ========================================================================
    public static final class Server {
        public final ForgeConfigSpec.IntValue configVersion;
        public final Commands commands;
        public final Compat compat;
        public final Progression progression;
        public final Caps caps;
        public final MobStats mobstats;
        public final Logging logging;
        public final Zeny zeny;
        public final Items items;

        Server(ForgeConfigSpec.Builder builder) {
            configVersion = builder.comment("Configuration version").defineInRange("config_version", 1, 1, 1000);
            this.commands = new Commands(builder);
            this.compat = new Compat(builder);
            this.progression = new Progression(builder);
            this.caps = new Caps(builder);
            this.mobstats = new MobStats(builder);
            this.logging = new Logging(builder);
            this.zeny = new Zeny(builder);
            this.items = new Items(builder);
        }

        public static final class Commands {
            public final ForgeConfigSpec.BooleanValue enableLegacyAliases;
            Commands(ForgeConfigSpec.Builder builder) {
                builder.comment("Command options").push("commands");
                enableLegacyAliases = builder.define("enable_legacy_aliases", false);
                builder.pop();
            }
        }

        public static final class Compat {
            public final ForgeConfigSpec.BooleanValue useVanillaAttributes;
            public final ForgeConfigSpec.BooleanValue useRagnarCompute;
            Compat(ForgeConfigSpec.Builder builder) {
                builder.comment("Compatibility options").push("compat");
                useVanillaAttributes = builder.define("use_vanilla_attributes", true);
                useRagnarCompute = builder.define("use_ragnar_compute", true);
                builder.pop();
            }
        }

        public static final class Progression {
            public final ForgeConfigSpec.DoubleValue expGlobalMultiplier;
            public final ForgeConfigSpec.DoubleValue jobExpGlobalMultiplier;
            public final ForgeConfigSpec.DoubleValue baseExpDeathPenaltyRate;
            public final ForgeConfigSpec.DoubleValue jobExpDeathPenaltyRate;
            public final ForgeConfigSpec.IntValue secondJobChangeMinJobLevel;
            public final ForgeConfigSpec.BooleanValue usePreRenewalStatPointCurve;
            public final ForgeConfigSpec.IntValue baseStatPoints;
            public final ForgeConfigSpec.IntValue pointsPerLevel;
            public final ForgeConfigSpec.DoubleValue skillToJobExpMultiplier;
            public final ForgeConfigSpec.DoubleValue skillToBaseExpMultiplier;
            public final ForgeConfigSpec.IntValue antiFarmTimeThreshold;
            public final ForgeConfigSpec.IntValue antiFarmRadiusChunks;
            public final ForgeConfigSpec.DoubleValue antiFarmMaxPenalty;
            public final ForgeConfigSpec.BooleanValue antiFarmSpawnReduction;

            Progression(ForgeConfigSpec.Builder builder) {
                builder.comment("Progression settings").push("progression");
                expGlobalMultiplier = builder.defineInRange("exp_global_multiplier", 1.0, 0.01, 100.0);
                jobExpGlobalMultiplier = builder.defineInRange("job_exp_global_multiplier", 1.0, 0.01, 100.0);
                baseExpDeathPenaltyRate = builder.defineInRange("base_exp_death_penalty_rate", 0.05, 0.0, 1.0);
                jobExpDeathPenaltyRate = builder.defineInRange("job_exp_death_penalty_rate", 0.05, 0.0, 1.0);
                secondJobChangeMinJobLevel = builder.defineInRange("second_job_change_min_job_level", 40, 1, 50);
                usePreRenewalStatPointCurve = builder.define("use_pre_renewal_stat_point_curve", true);
                skillToBaseExpMultiplier = builder.defineInRange("skill_to_base_exp_multiplier", 0.5, 0.0, 10.0);
                skillToJobExpMultiplier = builder.defineInRange("skill_to_job_exp_multiplier", 0.5, 0.0, 10.0);
                baseStatPoints = builder.defineInRange("base_stat_points", 48, 0, 500);
                pointsPerLevel = builder.defineInRange("points_per_level", 3, 0, 50);

                builder.push("anti_farm");
                antiFarmTimeThreshold = builder.defineInRange("time_threshold_minutes", 15, 1, 1440);
                antiFarmRadiusChunks = builder.defineInRange("radius_chunks", 2, 1, 10);
                antiFarmMaxPenalty = builder.defineInRange("max_penalty_limit", 0.1, 0.0, 1.0);
                antiFarmSpawnReduction = builder.define("spawn_reduction_enabled", true);
                builder.pop();
                builder.pop();
            }
        }

        public static final class Caps {
            public final ForgeConfigSpec.IntValue maxLevel;
            public final ForgeConfigSpec.IntValue maxJobLevel;
            public final ForgeConfigSpec.IntValue maxStatValue;
            public final ForgeConfigSpec.IntValue noviceMaxLevel;
            public final ForgeConfigSpec.IntValue noviceMaxJobLevel;

            Caps(ForgeConfigSpec.Builder builder) {
                builder.comment("Level and stat caps").push("caps");
                maxLevel = builder.defineInRange("max_level", 99, 1, 9999);
                maxJobLevel = builder.defineInRange("max_job_level", 50, 1, 9999);
                maxStatValue = builder.defineInRange("max_stat_value", 99, 99, 9999);
                noviceMaxLevel = builder.defineInRange("novice_max_level", 10, 1, 100);
                noviceMaxJobLevel = builder.defineInRange("novice_max_job_level", 10, 1, 100);
                builder.pop();
            }
        }

        public static final class MobStats {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.IntValue maxLevel;
            public final ForgeConfigSpec.DoubleValue eliteChance;
            public final ForgeConfigSpec.DoubleValue miniBossChance;
            public final ForgeConfigSpec.DoubleValue bossChance;
            public final ForgeConfigSpec.DoubleValue mvpChance;
            public final ForgeConfigSpec.DoubleValue naturalTierChanceScale;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> mobExcludeList;

            public final ForgeConfigSpec.DoubleValue hpBase;
            public final ForgeConfigSpec.DoubleValue vitToHp;
            public final ForgeConfigSpec.DoubleValue levelToHp;
            public final ForgeConfigSpec.DoubleValue atkBase;
            public final ForgeConfigSpec.DoubleValue strToAtk;
            public final ForgeConfigSpec.DoubleValue dexToAtk;
            public final ForgeConfigSpec.DoubleValue lukToAtk;
            public final ForgeConfigSpec.DoubleValue armorBase;
            public final ForgeConfigSpec.DoubleValue vitToArmor;
            public final ForgeConfigSpec.DoubleValue intToArmor;
            public final ForgeConfigSpec.DoubleValue agiToSpeed;
            public final ForgeConfigSpec.DoubleValue maxMovementSpeed;
            public final ForgeConfigSpec.DoubleValue lukToKbResist;

            public final ForgeConfigSpec.DoubleValue damagePerStr;
            public final ForgeConfigSpec.DoubleValue damagePerDex;
            public final ForgeConfigSpec.DoubleValue reductionPerVit;

            public final ForgeConfigSpec.IntValue basePointsNormal;
            public final ForgeConfigSpec.IntValue basePointsElite;
            public final ForgeConfigSpec.IntValue basePointsMiniBoss;
            public final ForgeConfigSpec.IntValue basePointsBoss;
            public final ForgeConfigSpec.IntValue basePointsMvp;
            public final ForgeConfigSpec.IntValue pointsPerLevelNormal;
            public final ForgeConfigSpec.IntValue pointsPerLevelElite;
            public final ForgeConfigSpec.IntValue pointsPerLevelMiniBoss;
            public final ForgeConfigSpec.IntValue pointsPerLevelBoss;
            public final ForgeConfigSpec.IntValue pointsPerLevelMvp;

            public final ForgeConfigSpec.DoubleValue healthMultNormal;
            public final ForgeConfigSpec.DoubleValue healthMultElite;
            public final ForgeConfigSpec.DoubleValue healthMultMiniBoss;
            public final ForgeConfigSpec.DoubleValue healthMultBoss;
            public final ForgeConfigSpec.DoubleValue healthMultMvp;
            public final ForgeConfigSpec.DoubleValue damageMultNormal;
            public final ForgeConfigSpec.DoubleValue damageMultElite;
            public final ForgeConfigSpec.DoubleValue damageMultMiniBoss;
            public final ForgeConfigSpec.DoubleValue damageMultBoss;
            public final ForgeConfigSpec.DoubleValue damageMultMvp;
            public final ForgeConfigSpec.DoubleValue defenseMultNormal;
            public final ForgeConfigSpec.DoubleValue defenseMultElite;
            public final ForgeConfigSpec.DoubleValue defenseMultMiniBoss;
            public final ForgeConfigSpec.DoubleValue defenseMultBoss;
            public final ForgeConfigSpec.DoubleValue defenseMultMvp;

            public final ForgeConfigSpec.BooleanValue renderNumericHealth;
            public final ForgeConfigSpec.EnumValue<LevelScalingMode> levelScalingMode;
            public final ForgeConfigSpec.EnumValue<ManualMobBackend> manualMobBackend;
            public final ForgeConfigSpec.EnumValue<ManualUncoveredBehavior> manualUncoveredBehavior;
            public final ForgeConfigSpec.EnumValue<AutomaticFallbackMode> manualFallbackAutomaticMode;
            public final ForgeConfigSpec.BooleanValue enableManualMobEditor;
            public final ForgeConfigSpec.BooleanValue enableManualMobDiscovery;
            public final ForgeConfigSpec.IntValue playerLevelRadius;
            public final ForgeConfigSpec.IntValue playerLevelVariance;

            public final DimensionConfig overworld;
            public final DimensionConfig nether;
            public final DimensionConfig end;

            public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionMinLevels;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> structureMinLevels;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> bossMinLevels;

            public final ForgeConfigSpec.DoubleValue partyScalingRadius;
            public final ForgeConfigSpec.DoubleValue partyHpMultiplier;
            public final ForgeConfigSpec.DoubleValue partyAtkMultiplier;

            MobStats(ForgeConfigSpec.Builder builder) {
                builder.comment("Mob scaling and tier system").push("mobstats");
                enabled = builder.define("enabled", true);
                maxLevel = builder.defineInRange("max_level", 0, 0, 100000);
                eliteChance = builder.defineInRange("elite_chance", 0.08, 0.0, 1.0);
                miniBossChance = builder.defineInRange("mini_boss_chance", 0.02, 0.0, 1.0);
                bossChance = builder.defineInRange("boss_chance", 0.01, 0.0, 1.0);
                mvpChance = builder.defineInRange("mvp_chance", 0.002, 0.0, 1.0);
                naturalTierChanceScale = builder.defineInRange("natural_tier_chance_scale", 0.2, 0.0, 1.0);
                mobExcludeList = builder.defineList("mob_exclude_list", List.of("minecraft:armor_stand", "minecraft:villager"), o -> o instanceof String);

                builder.push("attributes");
                hpBase = builder.defineInRange("hp_base", 20.0, 0.0, 1.0E9);
                vitToHp = builder.defineInRange("vit_to_hp", 5.0, 0.0, 1.0E6);
                levelToHp = builder.defineInRange("level_to_hp", 5.0, 0.0, 1.0E6);
                atkBase = builder.defineInRange("atk_base", 4.0, 0.0, 1.0E9);
                strToAtk = builder.defineInRange("str_to_atk", 1.0, 0.0, 1.0E6);
                dexToAtk = builder.defineInRange("dex_to_atk", 0.2, 0.0, 1.0E6);
                lukToAtk = builder.defineInRange("luk_to_atk", 0.1, 0.0, 1.0E6);
                armorBase = builder.defineInRange("armor_base", 0.0, 0.0, 1.0E9);
                vitToArmor = builder.defineInRange("vit_to_armor", 0.2, 0.0, 1.0E6);
                intToArmor = builder.defineInRange("int_to_armor", 0.1, 0.0, 1.0E6);
                agiToSpeed = builder.defineInRange("agi_to_speed", 0.0025, 0.0, 1.0);
                maxMovementSpeed = builder.defineInRange("max_movement_speed", 0.36, 0.05, 2.0);
                lukToKbResist = builder.defineInRange("luk_to_kb_resist", 0.002, 0.0, 1.0);
                builder.pop();

                builder.push("combat");
                damagePerStr = builder.defineInRange("damage_per_str", 0.01, 0.0, 100.0);
                damagePerDex = builder.defineInRange("damage_per_dex", 0.005, 0.0, 100.0);
                reductionPerVit = builder.defineInRange("reduction_per_vit", 0.003, 0.0, 1.0);
                builder.pop();

                builder.push("progression");
                basePointsNormal = builder.defineInRange("base_points_normal", 5, 0, 100000);
                basePointsElite = builder.defineInRange("base_points_elite", 20, 0, 100000);
                basePointsMiniBoss = builder.defineInRange("base_points_mini_boss", 30, 0, 100000);
                basePointsBoss = builder.defineInRange("base_points_boss", 40, 0, 100000);
                basePointsMvp = builder.defineInRange("base_points_mvp", 60, 0, 100000);
                pointsPerLevelNormal = builder.defineInRange("per_level_normal", 3, 0, 10000);
                pointsPerLevelElite = builder.defineInRange("per_level_elite", 5, 0, 10000);
                pointsPerLevelMiniBoss = builder.defineInRange("per_level_mini_boss", 6, 0, 10000);
                pointsPerLevelBoss = builder.defineInRange("per_level_boss", 8, 0, 10000);
                pointsPerLevelMvp = builder.defineInRange("per_level_mvp", 10, 0, 10000);
                builder.pop();

                builder.push("multipliers");
                healthMultNormal = builder.defineInRange("health_normal", 1.0, 0.0, 100.0);
                healthMultElite = builder.defineInRange("health_elite", 2.0, 0.0, 100.0);
                healthMultMiniBoss = builder.defineInRange("health_mini_boss", 3.0, 0.0, 100.0);
                healthMultBoss = builder.defineInRange("health_boss", 4.0, 0.0, 100.0);
                healthMultMvp = builder.defineInRange("health_mvp", 6.0, 0.0, 100.0);
                damageMultNormal = builder.defineInRange("damage_normal", 1.0, 0.0, 100.0);
                damageMultElite = builder.defineInRange("damage_elite", 1.5, 0.0, 100.0);
                damageMultMiniBoss = builder.defineInRange("damage_mini_boss", 2.0, 0.0, 100.0);
                damageMultBoss = builder.defineInRange("damage_boss", 2.5, 0.0, 100.0);
                damageMultMvp = builder.defineInRange("damage_mvp", 3.2, 0.0, 100.0);
                defenseMultNormal = builder.defineInRange("defense_normal", 1.0, 0.0, 100.0);
                defenseMultElite = builder.defineInRange("defense_elite", 1.5, 0.0, 100.0);
                defenseMultMiniBoss = builder.defineInRange("defense_mini_boss", 1.65, 0.0, 100.0);
                defenseMultBoss = builder.defineInRange("defense_boss", 1.8, 0.0, 100.0);
                defenseMultMvp = builder.defineInRange("defense_mvp", 2.2, 0.0, 100.0);
                builder.pop();

                builder.push("world_scaling");
                renderNumericHealth = builder.define("render_numeric_health", true);
                levelScalingMode = builder.defineEnum("level_scaling_mode", LevelScalingMode.DISTANCE);
                manualMobBackend = builder.defineEnum("manual_mob_backend", ManualMobBackend.DATAPACK);
                manualUncoveredBehavior = builder.defineEnum("manual_uncovered_behavior", ManualUncoveredBehavior.VANILLA);
                manualFallbackAutomaticMode = builder.defineEnum("manual_fallback_automatic_mode", AutomaticFallbackMode.DISTANCE);
                enableManualMobEditor = builder.define("enable_manual_mob_editor", false);
                enableManualMobDiscovery = builder.define("enable_manual_mob_discovery", true);
                playerLevelRadius = builder.defineInRange("player_level_radius", 64, 8, 256);
                playerLevelVariance = builder.defineInRange("player_level_variance", 2, 0, 100);

                overworld = new DimensionConfig(builder, "overworld", 1, 160,
                        () -> List.of("0-999=1-5", "1000-2499=5-10", "2500+=10-15"));

                nether = new DimensionConfig(builder, "nether", 30, 320,
                        () -> List.of("0-999=30-38", "1000+=38-48"));

                end = new DimensionConfig(builder, "end", 60, 420,
                        () -> List.of("0-999=60-70", "1000+=70-82"));

                builder.push("minimums");
                dimensionMinLevels = builder.defineList("dimension_min_levels", List.of(), o -> o instanceof String);
                structureMinLevels = builder.defineList("structure_min_levels", List.of(), o -> o instanceof String);
                bossMinLevels = builder.defineList("boss_min_levels", List.of(), o -> o instanceof String);
                builder.pop();
                builder.pop();

                builder.push("multiplayer_scaling");
                partyScalingRadius = builder.defineInRange("radius", 32.0, 1.0, 256.0);
                partyHpMultiplier = builder.defineInRange("hp_per_player", 0.4, 0.0, 10.0);
                partyAtkMultiplier = builder.defineInRange("atk_per_player", 0.2, 0.0, 10.0);
                builder.pop();
                builder.pop();
            }
        }

        public static final class Logging {
            public final ForgeConfigSpec.BooleanValue debug;
            public final ForgeConfigSpec.BooleanValue debugCombat;
            public final ForgeConfigSpec.BooleanValue debugPlayerData;
            public final ForgeConfigSpec.BooleanValue debugMobSpawns;
            public final ForgeConfigSpec.BooleanValue debugBossWorld;
            public final ForgeConfigSpec.BooleanValue debugMigration;
            public final ForgeConfigSpec.IntValue warnRateLimitSeconds;

            Logging(ForgeConfigSpec.Builder builder) {
                builder.comment("Logging and debug options").push("logging");
                debug = builder.define("debug", false);
                debugCombat = builder.define("debug_combat", false);
                debugPlayerData = builder.define("debug_player_data", false);
                debugMobSpawns = builder.define("debug_mob_spawns", false);
                debugBossWorld = builder.define("debug_boss_world", false);
                debugMigration = builder.define("debug_migration", false);
                warnRateLimitSeconds = builder.defineInRange("warn_rate_limit_seconds", 60, 1, 3600);
                builder.pop();
            }
        }

        public static final class Zeny {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.IntValue baseMobDrop;
            public final ForgeConfigSpec.DoubleValue levelMultiplier;
            public final ForgeConfigSpec.DoubleValue eliteMultiplier;
            public final ForgeConfigSpec.DoubleValue bossMultiplier;
            public final ForgeConfigSpec.DoubleValue lukBonusPerPoint;
            public final ForgeConfigSpec.LongValue maxDropPerKill;
            public final ForgeConfigSpec.IntValue villagerPriceMultiplier;

            public final ForgeConfigSpec.DoubleValue copperBaseChance;
            public final ForgeConfigSpec.DoubleValue silverBaseChance;
            public final ForgeConfigSpec.DoubleValue goldBaseChance;
            public final ForgeConfigSpec.DoubleValue eliteChanceMult;
            public final ForgeConfigSpec.DoubleValue bossChanceMult;
            public final ForgeConfigSpec.DoubleValue dropLukBonusFactor;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionMultipliers;
            public final ForgeConfigSpec.DoubleValue dimensionMultCap;

            Zeny(ForgeConfigSpec.Builder builder) {
                builder.comment("Economy settings").push("economy");
                enabled = builder.define("enabled", true);
                builder.push("rates");
                baseMobDrop = builder.defineInRange("base_mob_drop", 10, 0, 100000);
                levelMultiplier = builder.defineInRange("level_multiplier", 2.0, 0.0, 10000.0);
                eliteMultiplier = builder.defineInRange("elite_multiplier", 3.0, 1.0, 100.0);
                bossMultiplier = builder.defineInRange("boss_multiplier", 10.0, 1.0, 1000.0);
                lukBonusPerPoint = builder.defineInRange("luk_bonus_per_point", 0.5, 0.0, 100.0);
                maxDropPerKill = builder.defineInRange("max_drop_per_kill", 999_999L, 0L, 2_000_000_000L);
                villagerPriceMultiplier = builder.defineInRange("villager_price_multiplier", 10, 1, 100000);
                builder.pop();

                builder.push("drops");
                copperBaseChance = builder.defineInRange("copper_base_chance", 0.15, 0.0, 1.0);
                silverBaseChance = builder.defineInRange("silver_base_chance", 0.02, 0.0, 1.0);
                goldBaseChance = builder.defineInRange("gold_base_chance", 0.001, 0.0, 1.0);
                eliteChanceMult = builder.defineInRange("elite_chance_multiplier", 3.0, 1.0, 100.0);
                bossChanceMult = builder.defineInRange("boss_chance_multiplier", 10.0, 1.0, 1000.0);
                dropLukBonusFactor = builder.defineInRange("luk_bonus_factor", 0.005, 0.0, 1.0);
                dimensionMultipliers = builder.defineList("dimension_multipliers", List.of("minecraft:the_nether=1.5", "minecraft:the_end=2.0"), o -> o instanceof String);
                dimensionMultCap = builder.defineInRange("dimension_multiplier_cap", 5.0, 1.0, 100.0);
                builder.pop();
                builder.pop();
            }
        }

        public static final class Items {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.BooleanValue enableHeuristics;
            public final ForgeConfigSpec.BooleanValue blockEquipOnRestriction;
            public final ForgeConfigSpec.BooleanValue reduceDamageOnRestriction;
            public final ForgeConfigSpec.IntValue tickCheckInterval;
            public final ForgeConfigSpec.IntValue messageCooldownMs;
            public final ForgeConfigSpec.BooleanValue showTooltips;
            public final ForgeConfigSpec.DoubleValue penaltyDamage;

            public final ForgeConfigSpec.BooleanValue refineEnabled;
            public final ForgeConfigSpec.IntValue safeRefineLevel;
            public final ForgeConfigSpec.IntValue weaponBaseCost;
            public final ForgeConfigSpec.IntValue armorBaseCost;
            public final ForgeConfigSpec.IntValue costPerLevel;
            public final ForgeConfigSpec.DoubleValue weaponSuccessAfterSafe;
            public final ForgeConfigSpec.DoubleValue armorSuccessAfterSafe;
            public final ForgeConfigSpec.DoubleValue weaponSuccessPenaltyPerLevel;
            public final ForgeConfigSpec.DoubleValue armorSuccessPenaltyPerLevel;
            public final ForgeConfigSpec.DoubleValue minSuccessChance;
            public final ForgeConfigSpec.DoubleValue researchOrideconBonusPerLevel;

            Items(ForgeConfigSpec.Builder builder) {
                builder.comment("Items & Refine").push("items");
                enabled = builder.define("enabled", true);
                enableHeuristics = builder.define("enable_heuristics", true);
                blockEquipOnRestriction = builder.define("block_equip_on_restriction", true);
                reduceDamageOnRestriction = builder.define("reduce_damage_on_restriction", true);
                tickCheckInterval = builder.defineInRange("tick_check_interval", 5, 1, 40);
                messageCooldownMs = builder.defineInRange("message_cooldown_ms", 2000, 500, 10000);
                showTooltips = builder.define("show_tooltips", true);
                penaltyDamage = builder.defineInRange("penalty_damage", 0.0, 0.0, 100.0);

                builder.push("refine");
                refineEnabled = builder.define("enabled", true);
                safeRefineLevel = builder.defineInRange("safe_refine_level", 4, 0, 10);
                weaponBaseCost = builder.defineInRange("weapon_base_cost", 180, 0, 1_000_000);
                armorBaseCost = builder.defineInRange("armor_base_cost", 140, 0, 1_000_000);
                costPerLevel = builder.defineInRange("cost_per_level", 120, 0, 1_000_000);
                weaponSuccessAfterSafe = builder.defineInRange("weapon_success_after_safe", 0.75, 0.0, 1.0);
                armorSuccessAfterSafe = builder.defineInRange("armor_success_after_safe", 0.85, 0.0, 1.0);
                weaponSuccessPenaltyPerLevel = builder.defineInRange("weapon_success_penalty_per_level", 0.10, 0.0, 1.0);
                armorSuccessPenaltyPerLevel = builder.defineInRange("armor_success_penalty_per_level", 0.08, 0.0, 1.0);
                minSuccessChance = builder.defineInRange("min_success_chance", 0.25, 0.0, 1.0);
                researchOrideconBonusPerLevel = builder.defineInRange("research_oridecon_bonus_per_level", 0.02, 0.0, 0.5);
                builder.pop();
                builder.pop();
            }
        }
    }

    public enum LevelScalingMode {
        PLAYER_LEVEL, DISTANCE, MANUAL
    }

    public enum ManualMobBackend {
        INTERNAL, DATAPACK, HYBRID
    }

    public enum ManualUncoveredBehavior {
        VANILLA, FALLBACK_TO_AUTO
    }

    public enum AutomaticFallbackMode {
        PLAYER_LEVEL, DISTANCE
    }

    public static class DimensionConfig {
        public final ForgeConfigSpec.IntValue minFloor;
        public final ForgeConfigSpec.IntValue maxCap;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> distanceBands;

        public DimensionConfig(ForgeConfigSpec.Builder b, String dimName, int minFloor, int maxCap,
                               java.util.function.Supplier<List<String>> defaultDistanceBands) {
            b.push(dimName);
            this.minFloor = b.defineInRange("min_floor", minFloor, 1, 100000);
            this.maxCap = b.defineInRange("max_cap", maxCap, 1, 100000);
            this.distanceBands = b.defineList("distance_bands", defaultDistanceBands.get(), o -> o instanceof String);
            b.pop();
        }
    }
}
