package com.etema.ragnarmmo.common.config;

import com.etema.ragnarmmo.mobs.difficulty.DifficultyMode;
import com.etema.ragnarmmo.mobs.companion.CompanionRankMode;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

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
        public final Progression progression;
        public final Difficulty difficulty;
        public final Combat combat;
        public final Skills skills;
        public final LifeSkills lifeskills;
        public final Caps caps;
        public final Mobs mobs;
        public final Logging logging;
        public final Zeny zeny;
        public final Items items;

        Server(ForgeConfigSpec.Builder builder) {
            configVersion = builder.comment("Configuration version").defineInRange("config_version", 1, 1, 1000);
            this.progression = new Progression(builder);
            this.difficulty = new Difficulty(builder);
            this.combat = new Combat(builder);
            this.skills = new Skills(builder);
            this.lifeskills = new LifeSkills(builder);
            this.caps = new Caps(builder);
            this.mobs = new Mobs(builder);
            this.logging = new Logging(builder);
            this.zeny = new Zeny(builder);
            this.items = new Items(builder);
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

        public static final class Difficulty {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.EnumValue<DifficultyMode> mode;
            public final ForgeConfigSpec.IntValue maxLevel;
            public final RankChances rankChances;
            public final PlayerLevel playerLevel;
            public final DimensionConfig overworld;
            public final DimensionConfig nether;
            public final DimensionConfig end;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> structures;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> specialMobs;

            Difficulty(ForgeConfigSpec.Builder builder) {
                builder.comment("V2 mob difficulty configuration").push("difficulty");
                enabled = builder.define("enabled", true);
                mode = builder.defineEnum("mode", DifficultyMode.DISTANCE);
                maxLevel = builder.defineInRange("max_level", 160, 1, 100000);
                rankChances = new RankChances(builder);
                playerLevel = new PlayerLevel(builder);

                builder.push("dimensions");
                overworld = new DimensionConfig(builder, "overworld", 1, 160,
                        () -> List.of(
                                "0-999=1-5",
                                "1000-1999=5-10",
                                "2000-3499=10-18",
                                "3500-5999=18-30",
                                "6000-8999=30-45",
                                "9000-12999=45-65",
                                "13000-19999=65-90",
                                "20000-29999=90-120",
                                "30000+=120-160"));
                nether = new DimensionConfig(builder, "nether", 30, 320,
                        () -> List.of("0-999=30-38", "1000+=38-48"));
                end = new DimensionConfig(builder, "end", 60, 420,
                        () -> List.of("0-999=60-70", "1000+=70-82"));
                builder.pop();

                structures = builder.comment("Structure difficulty rules. Format: structure_id=min_level=70,min_rank=ELITE")
                        .defineList("structures", List.of(), value -> value instanceof String);
                specialMobs = builder.comment("Special mob rules. Format: entity_type_id=rank=BOSS,min_level=99")
                        .defineList("special_mobs", List.of(
                                "minecraft:wither=rank=MINI_BOSS,min_level=80",
                                "minecraft:warden=rank=BOSS,min_level=90",
                                "minecraft:ender_dragon=rank=BOSS,min_level=99"),
                                value -> value instanceof String);
                builder.pop();
            }

            public static final class RankChances {
                public final ForgeConfigSpec.DoubleValue elite;

                RankChances(ForgeConfigSpec.Builder builder) {
                    builder.push("rank_chances");
                    elite = builder.defineInRange("elite", 0.08, 0.0, 1.0);
                    builder.pop();
                }
            }

            public static final class PlayerLevel {
                public final ForgeConfigSpec.IntValue radius;
                public final ForgeConfigSpec.IntValue variance;

                PlayerLevel(ForgeConfigSpec.Builder builder) {
                    builder.push("player_level");
                    radius = builder.defineInRange("radius", 64, 8, 256);
                    variance = builder.defineInRange("variance", 2, 0, 100);
                    builder.pop();
                }
            }
        }

        public static final class Combat {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.DoubleValue mobDamagePerStr;
            public final ForgeConfigSpec.DoubleValue mobDamagePerDex;
            public final ForgeConfigSpec.DoubleValue mobReductionPerVit;

            Combat(ForgeConfigSpec.Builder builder) {
                builder.comment("V2 combat configuration").push("combat");
                enabled = builder.define("enabled", true);
                builder.push("mob_damage");
                mobDamagePerStr = builder.defineInRange("damage_per_str", 0.01, 0.0, 100.0);
                mobDamagePerDex = builder.defineInRange("damage_per_dex", 0.005, 0.0, 100.0);
                builder.pop();
                builder.push("defense");
                mobReductionPerVit = builder.defineInRange("reduction_per_vit", 0.003, 0.0, 1.0);
                builder.pop();
                builder.pop();
            }
        }

        public static final class Skills {
            public final ForgeConfigSpec.BooleanValue requireAuthoredDefinitions;

            Skills(ForgeConfigSpec.Builder builder) {
                builder.comment("V2 skills configuration").push("skills");
                requireAuthoredDefinitions = builder.define("require_authored_definitions", true);
                builder.pop();
            }
        }

        public static final class LifeSkills {
            public final ForgeConfigSpec.BooleanValue enabled;

            LifeSkills(ForgeConfigSpec.Builder builder) {
                builder.comment("V2 life skills configuration").push("lifeskills");
                enabled = builder.define("enabled", true);
                builder.pop();
            }
        }

        public static final class Mobs {
            public final ForgeConfigSpec.BooleanValue enabled;
            public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludeList;
            public final ForgeConfigSpec.BooleanValue renderNumericHealth;
            public final DefaultProfile defaultProfile;
            public final Attributes attributes;
            public final Companions companions;
            public final ForgeConfigSpec.DoubleValue partyScalingRadius;
            public final ForgeConfigSpec.DoubleValue partyHpMultiplier;
            public final ForgeConfigSpec.DoubleValue partyAtkMultiplier;

            Mobs(ForgeConfigSpec.Builder builder) {
                builder.comment("V2 mob runtime profile configuration").push("mobs");
                enabled = builder.define("enabled", true);
                excludeList = builder.defineList("exclude_list", List.of("minecraft:armor_stand", "minecraft:villager"), o -> o instanceof String);
                renderNumericHealth = builder.define("render_numeric_health", true);
                defaultProfile = new DefaultProfile(builder);
                attributes = new Attributes(builder);
                companions = new Companions(builder);

                builder.push("multiplayer_scaling");
                partyScalingRadius = builder.defineInRange("radius", 32.0, 1.0, 256.0);
                partyHpMultiplier = builder.defineInRange("hp_per_player", 0.4, 0.0, 10.0);
                partyAtkMultiplier = builder.defineInRange("atk_per_player", 0.2, 0.0, 10.0);
                builder.pop();
                builder.pop();
            }

            public static final class DefaultProfile {
                public final ForgeConfigSpec.ConfigValue<String> race;
                public final ForgeConfigSpec.ConfigValue<String> element;
                public final ForgeConfigSpec.ConfigValue<String> size;
                public final ForgeConfigSpec.IntValue maxHp;
                public final ForgeConfigSpec.IntValue atkMin;
                public final ForgeConfigSpec.IntValue atkMax;
                public final ForgeConfigSpec.IntValue def;
                public final ForgeConfigSpec.IntValue mdef;
                public final ForgeConfigSpec.IntValue hit;
                public final ForgeConfigSpec.IntValue flee;
                public final ForgeConfigSpec.IntValue crit;
                public final ForgeConfigSpec.IntValue aspd;
                public final ForgeConfigSpec.DoubleValue moveSpeed;

                DefaultProfile(ForgeConfigSpec.Builder builder) {
                    builder.push("default_profile");
                    race = builder.define("race", "unknown");
                    element = builder.define("element", "neutral");
                    size = builder.define("size", "medium");
                    maxHp = builder.defineInRange("max_hp", 20, 1, 1_000_000_000);
                    atkMin = builder.defineInRange("atk_min", 2, 0, 1_000_000_000);
                    atkMax = builder.defineInRange("atk_max", 4, 0, 1_000_000_000);
                    def = builder.defineInRange("def", 0, 0, 1_000_000_000);
                    mdef = builder.defineInRange("mdef", 0, 0, 1_000_000_000);
                    hit = builder.defineInRange("hit", 10, 0, 1_000_000_000);
                    flee = builder.defineInRange("flee", 5, 0, 1_000_000_000);
                    crit = builder.defineInRange("crit", 1, 0, 1_000_000_000);
                    aspd = builder.defineInRange("aspd", 150, 1, 1_000_000_000);
                    moveSpeed = builder.defineInRange("move_speed", 0.20D, 0.0001D, 10.0D);
                    builder.pop();
                }
            }

            public static final class Attributes {
                public final ForgeConfigSpec.DoubleValue hpPerLevel;
                public final ForgeConfigSpec.DoubleValue atkMinPerLevel;
                public final ForgeConfigSpec.DoubleValue atkMaxExtraPerLevel;
                public final ForgeConfigSpec.DoubleValue defPerLevel;
                public final ForgeConfigSpec.DoubleValue mdefPerLevel;
                public final ForgeConfigSpec.DoubleValue hitPerLevel;
                public final ForgeConfigSpec.DoubleValue fleePerLevel;
                public final ForgeConfigSpec.DoubleValue aspdPerLevel;
                public final ForgeConfigSpec.DoubleValue moveSpeedPerLevel;
                public final ForgeConfigSpec.DoubleValue moveSpeedCap;

                Attributes(ForgeConfigSpec.Builder builder) {
                    builder.push("attributes");
                    hpPerLevel = builder.defineInRange("hp_per_level", 8.0D, 0.0D, 1.0E6D);
                    atkMinPerLevel = builder.defineInRange("atk_min_per_level", 1.0D, 0.0D, 1.0E6D);
                    atkMaxExtraPerLevel = builder.defineInRange("atk_max_extra_per_level", 0.5D, 0.0D, 1.0E6D);
                    defPerLevel = builder.defineInRange("def_per_level", 0.333D, 0.0D, 1.0E6D);
                    mdefPerLevel = builder.defineInRange("mdef_per_level", 0.25D, 0.0D, 1.0E6D);
                    hitPerLevel = builder.defineInRange("hit_per_level", 2.0D, 0.0D, 1.0E6D);
                    fleePerLevel = builder.defineInRange("flee_per_level", 1.0D, 0.0D, 1.0E6D);
                    aspdPerLevel = builder.defineInRange("aspd_per_level", 0.5D, 0.0D, 1.0E6D);
                    moveSpeedPerLevel = builder.defineInRange("move_speed_per_level", 0.002D, 0.0D, 1.0D);
                    moveSpeedCap = builder.defineInRange("move_speed_cap", 0.36D, 0.0001D, 10.0D);
                    builder.pop();
                }
            }

            public static final class Companions {
                public final ForgeConfigSpec.EnumValue<CompanionRankMode> rankMode;
                public final ForgeConfigSpec.DoubleValue syncRadius;
                public final ForgeConfigSpec.BooleanValue deferUntilOwnerOnline;

                Companions(ForgeConfigSpec.Builder builder) {
                    builder.push("companions");
                    rankMode = builder.defineEnum("rank_mode", CompanionRankMode.NORMAL_ONLY);
                    syncRadius = builder.defineInRange("sync_radius", 64.0D, 1.0D, 256.0D);
                    deferUntilOwnerOnline = builder.define("defer_until_owner_online", true);
                    builder.pop();
                }
            }
        }

        public static final class Logging {
            public final ForgeConfigSpec.BooleanValue debug;
            public final ForgeConfigSpec.BooleanValue debugCombat;
            public final ForgeConfigSpec.BooleanValue debugPlayerData;
            public final ForgeConfigSpec.BooleanValue debugMobSpawns;
            public final ForgeConfigSpec.BooleanValue debugBossWorld;
            public final ForgeConfigSpec.BooleanValue debugRuntime;
            public final ForgeConfigSpec.IntValue warnRateLimitSeconds;

            Logging(ForgeConfigSpec.Builder builder) {
                builder.comment("Logging and debug options").push("logging");
                debug = builder.define("debug", false);
                debugCombat = builder.define("debug_combat", false);
                debugPlayerData = builder.define("debug_player_data", false);
                debugMobSpawns = builder.define("debug_mob_spawns", false);
                debugBossWorld = builder.define("debug_boss_world", false);
                debugRuntime = builder.define("debug_runtime", false);
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
            public final ForgeConfigSpec.DoubleValue eliteDropMultiplier;
            public final ForgeConfigSpec.DoubleValue miniBossDropMultiplier;
            public final ForgeConfigSpec.DoubleValue bossDropMultiplier;
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
                eliteDropMultiplier = builder.defineInRange("elite_drop_multiplier", 3.0, 1.0, 100.0);
                miniBossDropMultiplier = builder.defineInRange("mini_boss_drop_multiplier", 6.0, 1.0, 1000.0);
                bossDropMultiplier = builder.defineInRange("boss_drop_multiplier", 10.0, 1.0, 1000.0);
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
