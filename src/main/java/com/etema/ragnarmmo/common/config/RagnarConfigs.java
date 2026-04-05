package com.etema.ragnarmmo.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Unified configuration for RagnarMMO.
 * Only 2 files: ragnarmmo-client.toml (HUD) and ragnarmmo-server.toml (all
 * server rules).
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

        private RagnarConfigs() {
        }

        // ========================================================================
        // CLIENT CONFIG - HUD only (runs on client side)
        // ========================================================================
        public static final class Client {
                public final Hud hud;
                public final PartyHud partyHud;

                Client(ForgeConfigSpec.Builder builder) {
                        this.hud = new Hud(builder);
                        this.partyHud = new PartyHud(builder);
                }

                public enum HudAnchor {
                        TOP_LEFT,
                        TOP_RIGHT,
                        BOTTOM_LEFT,
                        BOTTOM_RIGHT
                }

                public static final class Hud {
                        public final ForgeConfigSpec.BooleanValue enabled;
                        public final ForgeConfigSpec.DoubleValue scale;
                        public final ForgeConfigSpec.IntValue width;
                        public final ForgeConfigSpec.IntValue backgroundAlpha;

                        public final HudComponent status;

                        Hud(ForgeConfigSpec.Builder builder) {
                                builder.comment("HUD overlay configuration").push("hud");

                                enabled = builder
                                                .comment("Show the experience HUD overlay")
                                                .define("enabled", true);

                                scale = builder
                                                .comment("Scale of the HUD overlay")
                                                .defineInRange("scale", 1.0, 0.5, 3.0);

                                width = builder
                                                .comment("Width of the HUD panel in pixels")
                                                .defineInRange("width", 150, 120, 400);

                                backgroundAlpha = builder
                                                .comment("DEPRECATED: Background opacity (0-255). Use per-component settings.")
                                                .defineInRange("background_alpha", 0, 0, 255);

                                status = new HudComponent(builder, "status", 0.0, 0.0);

                                builder.pop();
                        }

                        public static class HudComponent {
                                public final ForgeConfigSpec.DoubleValue anchorX;
                                public final ForgeConfigSpec.DoubleValue anchorY;
                                public final ForgeConfigSpec.BooleanValue enabled;
                                public final ForgeConfigSpec.DoubleValue scale;
                                public final ForgeConfigSpec.IntValue backgroundAlpha;
                                public final ForgeConfigSpec.BooleanValue showBackground;

                                public HudComponent(ForgeConfigSpec.Builder builder, String name, double x, double y) {
                                        builder.push(name);
                                        anchorX = builder.defineInRange("anchor_x", x, 0.0, 1.0);
                                        anchorY = builder.defineInRange("anchor_y", y, 0.0, 1.0);
                                        enabled = builder.define("enabled", true);
                                        scale = builder.defineInRange("scale", 1.0, 0.1, 5.0);
                                        backgroundAlpha = builder.defineInRange("background_alpha", 100, 0, 255);
                                        showBackground = builder.define("show_background", true);
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

                                enabled = builder
                                                .comment("Enable the party HUD overlay")
                                                .define("enabled", true);

                                scale = builder
                                                .comment("Scale of the party HUD overlay")
                                                .defineInRange("scale", 1.0, 0.75, 1.5);

                                anchor = builder
                                                .comment("Anchor corner of the party HUD overlay")
                                                .defineEnum("anchor", HudAnchor.TOP_LEFT);

                                xOffset = builder
                                                .comment("Horizontal offset in pixels (from the anchored corner)")
                                                .defineInRange("x_offset", 10, -1000, 1000);

                                yOffset = builder
                                                .comment("Vertical offset in pixels (from the anchored corner)")
                                                .defineInRange("y_offset", 30, -1000, 1000);

                                showSelf = builder
                                                .comment("Show your own entry in the party HUD")
                                                .define("show_self", false);

                                builder.pop();
                        }
                }
        }

        // ========================================================================
        // SERVER CONFIG - All server-side rules
        // ========================================================================
        public static final class Server {
                public final Commands commands;
                public final Compat compat;
                public final Progression progression;
                public final Caps caps;
                public final MobStats mobstats;
                public final Logging logging;
                public final Zeny zeny;

                Server(ForgeConfigSpec.Builder builder) {
                        this.commands = new Commands(builder);
                        this.compat = new Compat(builder);
                        this.progression = new Progression(builder);
                        this.caps = new Caps(builder);
                        this.mobstats = new MobStats(builder);
                        this.logging = new Logging(builder);
                        this.zeny = new Zeny(builder);
                }

                public static final class Commands {
                        public final ForgeConfigSpec.BooleanValue enableLegacyAliases;

                        Commands(ForgeConfigSpec.Builder builder) {
                                builder.comment("Command options").push("commands");

                                enableLegacyAliases = builder
                                                .comment("If true, register legacy root aliases (/stats, /skills, /party, /cart, /mobstats, /pc)")
                                                .define("enable_legacy_aliases", false);

                                builder.pop();
                        }
                }

                public static final class Compat {
                        public final ForgeConfigSpec.BooleanValue useVanillaAttributes;
                        public final ForgeConfigSpec.BooleanValue useRagnarCompute;

                        Compat(ForgeConfigSpec.Builder builder) {
                                builder.comment("Compatibility options").push("compat");

                                useVanillaAttributes = builder
                                                .comment("If true, base stats affect synchronized vanilla attributes")
                                                .define("use_vanilla_attributes", true);

                                useRagnarCompute = builder
                                                .comment("If true, use the advanced Ragnarok calculation pipeline")
                                                .define("use_ragnar_compute", true);

                                builder.pop();
                        }
                }

                public static final class Progression {
                        public final ForgeConfigSpec.DoubleValue expGlobalMultiplier;
                        public final ForgeConfigSpec.DoubleValue jobExpGlobalMultiplier;
                        public final ForgeConfigSpec.IntValue baseStatPoints;
                        public final ForgeConfigSpec.IntValue pointsPerLevel;
                        public final ForgeConfigSpec.DoubleValue skillToBaseExpMultiplier;
                        public final ForgeConfigSpec.DoubleValue skillToJobExpMultiplier;
                        public final ForgeConfigSpec.DoubleValue lifePointsToBaseExpMultiplier;
                        public final ForgeConfigSpec.DoubleValue lifePointsToJobExpMultiplier;

                        Progression(ForgeConfigSpec.Builder builder) {
                                builder.comment("Experience and stat point progression").push("progression");

                                expGlobalMultiplier = builder
                                                .comment("Global multiplier for all base EXP gains")
                                                .defineInRange("exp_global_multiplier", 1.0, 0.01, 100.0);

                                jobExpGlobalMultiplier = builder
                                                .comment("Global multiplier for all job EXP gains")
                                                .defineInRange("job_exp_global_multiplier", 1.0, 0.01, 100.0);

                                skillToBaseExpMultiplier = builder
                                                .comment("Multiplier for skill XP contributing to base level")
                                                .defineInRange("skill_to_base_exp_multiplier", 0.5, 0.0, 10.0);

                                skillToJobExpMultiplier = builder
                                                .comment("Multiplier for skill XP contributing to job level")
                                                .defineInRange("skill_to_job_exp_multiplier", 0.5, 0.0, 10.0);

                                lifePointsToBaseExpMultiplier = builder
                                                .comment("Multiplier for life skill points contributing to base level")
                                                .defineInRange("life_points_to_base_exp_multiplier", 2.0, 0.0, 100.0);

                                lifePointsToJobExpMultiplier = builder
                                                .comment("Multiplier for life skill points contributing to job level")
                                                .defineInRange("life_points_to_job_exp_multiplier", 2.0, 0.0, 100.0);

                                baseStatPoints = builder
                                                .comment("Base stat points granted to new characters")
                                                .defineInRange("base_stat_points", 20, 0, 500);

                                pointsPerLevel = builder
                                                .comment("Stat points gained per level up")
                                                .defineInRange("points_per_level", 3, 0, 50);

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

                                maxLevel = builder
                                                .comment("Maximum base level")
                                                .defineInRange("max_level", 50, 1, 9999);

                                maxJobLevel = builder
                                                .comment("Maximum job level")
                                                .defineInRange("max_job_level", 50, 1, 9999);

                                maxStatValue = builder
                                                .comment("Maximum value for any stat")
                                                .defineInRange("max_stat_value", 999, 99, 9999);

                                noviceMaxLevel = builder
                                                .comment("Maximum level for Novice class")
                                                .defineInRange("novice_max_level", 10, 1, 100);

                                noviceMaxJobLevel = builder
                                                .comment("Maximum job level for Novice class")
                                                .defineInRange("novice_max_job_level", 10, 1, 100);

                                builder.pop();
                        }
                }

                public static final class MobStats {
                        public final ForgeConfigSpec.BooleanValue enabled;
                        public final ForgeConfigSpec.DoubleValue eliteChance;
                        public final ForgeConfigSpec.DoubleValue bossChance;
                        public final ForgeConfigSpec.BooleanValue usePlayerLevelAnchor;
                        public final ForgeConfigSpec.IntValue levelMin;
                        public final ForgeConfigSpec.IntValue levelMax;
                        public final ForgeConfigSpec.IntValue playerLevelRange;
                        public final ForgeConfigSpec.DoubleValue playerLevelWeight;

                        MobStats(ForgeConfigSpec.Builder builder) {
                                builder.comment("Mob scaling and tier system").push("mobstats");

                                enabled = builder
                                                .comment("Enable mob stat scaling system")
                                                .define("enabled", true);

                                eliteChance = builder
                                                .comment("Chance for a mob to spawn as Elite tier")
                                                .defineInRange("elite_chance", 0.08, 0.0, 1.0);

                                bossChance = builder
                                                .comment("Chance for a mob to spawn as Boss tier")
                                                .defineInRange("boss_chance", 0.01, 0.0, 1.0);

                                usePlayerLevelAnchor = builder
                                                .comment("If true, mob levels scale with nearby player level")
                                                .define("use_player_level_anchor", true);

                                levelMin = builder
                                                .comment("Minimum mob level")
                                                .defineInRange("level_min", 1, 1, 100000);

                                levelMax = builder
                                                .comment("Maximum mob level")
                                                .defineInRange("level_max", 20, 1, 100000);

                                playerLevelRange = builder
                                                .comment("Level variance when using player level anchor")
                                                .defineInRange("player_level_range", 3, 0, 1000);

                                playerLevelWeight = builder
                                                .comment("Weight of player level in calculations (0-1)")
                                                .defineInRange("player_level_weight", 0.7, 0.0, 1.0);

                                builder.pop();
                        }
                }

                public static final class Logging {
                        public final ForgeConfigSpec.BooleanValue debug;
                        public final ForgeConfigSpec.IntValue warnRateLimitSeconds;

                        Logging(ForgeConfigSpec.Builder builder) {
                                builder.comment("Logging and debug options").push("logging");

                                debug = builder
                                                .comment("Enable debug logging")
                                                .define("debug", false);

                                warnRateLimitSeconds = builder
                                                .comment("Rate limit for warning messages (seconds)")
                                                .defineInRange("warn_rate_limit_seconds", 60, 1, 3600);

                                builder.pop();
                        }
                }

                // ── Zeny Economy ──────────────────────────────────────────────────────────
                public static final class Zeny {
                        public final ForgeConfigSpec.BooleanValue enabled;
                        public final ForgeConfigSpec.IntValue baseMobDrop;
                        public final ForgeConfigSpec.DoubleValue levelMultiplier;
                        public final ForgeConfigSpec.DoubleValue eliteMultiplier;
                        public final ForgeConfigSpec.DoubleValue bossMultiplier;
                        public final ForgeConfigSpec.DoubleValue lukBonusPerPoint;
                        public final ForgeConfigSpec.LongValue maxDropPerKill;
                        public final ForgeConfigSpec.IntValue villagerPriceMultiplier;

                        Zeny(ForgeConfigSpec.Builder builder) {
                                builder.comment("Zeny economy settings (RO-style currency)").push("zeny");

                                enabled = builder
                                                .comment("Enable the Zeny economy system (mobs drop Zeny, villagers price in Zeny)")
                                                .define("enabled", true);

                                baseMobDrop = builder
                                                .comment("Base Zeny dropped by a level-1 mob")
                                                .defineInRange("base_mob_drop", 10, 0, 100000);

                                levelMultiplier = builder
                                                .comment("Additional Zeny per mob level (mob_level * this + base)")
                                                .defineInRange("level_multiplier", 2.0, 0.0, 10000.0);

                                eliteMultiplier = builder
                                                .comment("Zeny multiplier for Elite-tier mobs")
                                                .defineInRange("elite_multiplier", 3.0, 1.0, 100.0);

                                bossMultiplier = builder
                                                .comment("Zeny multiplier for Boss-tier mobs")
                                                .defineInRange("boss_multiplier", 10.0, 1.0, 1000.0);

                                lukBonusPerPoint = builder
                                                .comment("Extra Zeny % per LUK point (0.5 = +0.5% per LUK)")
                                                .defineInRange("luk_bonus_per_point", 0.5, 0.0, 100.0);

                                maxDropPerKill = builder
                                                .comment("Maximum Zeny that can drop from a single kill (0 = no cap)")
                                                .defineInRange("max_drop_per_kill", 999_999L, 0L, 2_000_000_000L);

                                villagerPriceMultiplier = builder
                                                .comment("Villager prices in Zeny = emerald_count * this value")
                                                .defineInRange("villager_price_multiplier", 10, 1, 100000);

                                builder.pop();
                        }
                }
        }
}
