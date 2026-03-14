package com.etema.ragnarmmo.roitems.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for the RO Items system.
 * Uses ForgeConfigSpec for automatic config file generation and reloading.
 */
public final class RoItemsConfig {

    public static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;

    static {
        Pair<Config, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private RoItemsConfig() {}

    public static class Config {
        /**
         * Master switch for the RO items system.
         */
        public final ForgeConfigSpec.BooleanValue enabled;

        /**
         * Whether to generate fallback rules for items without explicit JSON.
         */
        public final ForgeConfigSpec.BooleanValue enableHeuristics;

        /**
         * Whether to block equipping items when requirements are not met.
         */
        public final ForgeConfigSpec.BooleanValue blockEquipOnRestriction;

        /**
         * Whether to reduce weapon damage when requirements are not met.
         */
        public final ForgeConfigSpec.BooleanValue reduceDamageOnRestriction;

        /**
         * Interval in ticks between equipment change checks.
         */
        public final ForgeConfigSpec.IntValue tickCheckInterval;

        /**
         * Cooldown in milliseconds between restriction warning messages.
         */
        public final ForgeConfigSpec.IntValue messageCooldownMs;

        /**
         * Whether to show RO-style tooltips on items.
         */
        public final ForgeConfigSpec.BooleanValue showTooltips;

        /**
         * Damage dealt when weapon requirements are not met.
         * 0.0 = no damage (like not having a weapon equipped)
         * 1.0 = minimum damage (RO-style penalty)
         */
        public final ForgeConfigSpec.DoubleValue penaltyDamage;

        Config(ForgeConfigSpec.Builder builder) {
            builder.comment("RO-style Item Attributes and Restrictions")
                    .push("roitems");

            enabled = builder
                    .comment("Enable the RO items system")
                    .define("enabled", true);

            enableHeuristics = builder
                    .comment("Generate fallback rules for TieredItem/ArmorItem without explicit JSON rules")
                    .define("enable_heuristics", true);

            blockEquipOnRestriction = builder
                    .comment("Block equipping items when level/class requirements are not met")
                    .define("block_equip_on_restriction", true);

            reduceDamageOnRestriction = builder
                    .comment("Reduce weapon damage to 1 when requirements are not met")
                    .define("reduce_damage_on_restriction", true);

            tickCheckInterval = builder
                    .comment("Ticks between equipment change checks (lower = more responsive, higher = better performance)")
                    .defineInRange("tick_check_interval", 5, 1, 40);

            messageCooldownMs = builder
                    .comment("Cooldown in milliseconds between restriction warning messages")
                    .defineInRange("message_cooldown_ms", 2000, 500, 10000);

            showTooltips = builder
                    .comment("Show RO-style tooltip information on items")
                    .define("show_tooltips", true);

            penaltyDamage = builder
                    .comment("Damage dealt when weapon requirements are not met (0 = no damage, 1 = minimum damage)")
                    .defineInRange("penalty_damage", 0.0, 0.0, 100.0);

            builder.pop();
        }
    }

    // Convenience accessors

    public static boolean isEnabled() {
        return CONFIG.enabled.get();
    }

    public static boolean areHeuristicsEnabled() {
        return CONFIG.enableHeuristics.get();
    }

    public static boolean shouldBlockEquip() {
        return CONFIG.blockEquipOnRestriction.get();
    }

    public static boolean shouldReduceDamage() {
        return CONFIG.reduceDamageOnRestriction.get();
    }

    public static int getTickCheckInterval() {
        return CONFIG.tickCheckInterval.get();
    }

    public static int getMessageCooldownMs() {
        return CONFIG.messageCooldownMs.get();
    }

    public static boolean shouldShowTooltips() {
        return CONFIG.showTooltips.get();
    }

    public static double getPenaltyDamage() {
        return CONFIG.penaltyDamage.get();
    }
}
