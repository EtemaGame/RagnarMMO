package com.etema.ragnarmmo.system.economy.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class ZenyConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue COPPER_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SILVER_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue GOLD_BASE_CHANCE;

    public static final ForgeConfigSpec.DoubleValue ELITE_CHANCE_MULT;
    public static final ForgeConfigSpec.DoubleValue BOSS_CHANCE_MULT;

    public static final ForgeConfigSpec.DoubleValue LUK_BONUS_FACTOR;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_MULTIPLIERS;
    public static final ForgeConfigSpec.DoubleValue DIMENSION_MULT_CAP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("RagnarMMO Economy - Zeny Drop Configuration").push("zeny_drops");

        COPPER_BASE_CHANCE = b.comment("Base chance for a normal mob to drop Copper Zeny (0.0 to 1.0)")
                .defineInRange("copper_base_chance", 0.15D, 0.0D, 1.0D);
        SILVER_BASE_CHANCE = b.comment("Base chance for a normal mob to drop Silver Zeny (0.0 to 1.0)")
                .defineInRange("silver_base_chance", 0.02D, 0.0D, 1.0D);
        GOLD_BASE_CHANCE = b.comment("Base chance for a normal mob to drop Gold Zeny (0.0 to 1.0)")
                .defineInRange("gold_base_chance", 0.001D, 0.0D, 1.0D);

        ELITE_CHANCE_MULT = b.comment("Multiplier for drop chances when the mob is an Elite")
                .defineInRange("elite_chance_multiplier", 3.0D, 1.0D, 100.0D);
        BOSS_CHANCE_MULT = b.comment("Multiplier for drop chances when the mob is a Boss")
                .defineInRange("boss_chance_multiplier", 10.0D, 1.0D, 1000.0D);

        LUK_BONUS_FACTOR = b.comment("How much LUK influences drop chances (effective_chance = base * (1 + luk * factor))")
                .defineInRange("luk_bonus_factor", 0.005D, 0.0D, 1.0D);

        DIMENSION_MULTIPLIERS = b.comment("List of dimension multipliers in format 'dimension_id=multiplier'",
                "Example: 'minecraft:the_nether=1.5', 'minecraft:the_end=2.0'")
                .defineList("dimension_multipliers", List.of("minecraft:the_nether=1.5", "minecraft:the_end=2.0"), o -> o instanceof String);

        DIMENSION_MULT_CAP = b.comment("Maximum multiplier cap for dimensions to prevent exploits")
                .defineInRange("dimension_multiplier_cap", 5.0D, 1.0D, 100.0D);

        b.pop();
        SPEC = b.build();
    }

    private ZenyConfig() {}
}
