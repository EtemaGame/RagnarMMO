package com.etema.ragnarmmo.system.economy;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.roitems.ZenyItems;
import com.etema.ragnarmmo.system.economy.config.ZenyConfig;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZenyDropManager {
    private static final Map<ResourceLocation, Double> DIM_MULT_CACHE = new HashMap<>();
    private static long lastConfigCheck = 0;

    public static List<ItemStack> calculateDrops(LivingEntity killed, Player killer, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        
        // 1. Get Mob Tier
        MobTier tier = MobStatsProvider.get(killed).map(stats -> stats.getTier()).orElse(MobTier.NORMAL);
        
        // 2. Get Dimension Multiplier
        double dimMult = getDimensionMultiplier(killed.level().dimension().location());
        
        // 3. Get Luck Bonus
        double luckBonus = killer.getCapability(PlayerStatsProvider.CAP).map(stats -> 1.0 + (stats.getLUK() * ZenyConfig.LUK_BONUS_FACTOR.get())).orElse(1.0);
        
        // 4. Calculate final multipliers
        double tierMult = switch (tier) {
            case ELITE -> ZenyConfig.ELITE_CHANCE_MULT.get();
            case MINI_BOSS -> ZenyConfig.ELITE_CHANCE_MULT.get() * 1.75D;
            case BOSS -> ZenyConfig.BOSS_CHANCE_MULT.get();
            case MVP -> ZenyConfig.BOSS_CHANCE_MULT.get() * 1.5D;
            default -> 1.0;
        };

        double finalMult = dimMult * luckBonus * tierMult;
        int copperMax = switch (tier) {
            case NORMAL -> 2;
            case ELITE -> 5;
            case MINI_BOSS -> 8;
            case BOSS -> 12;
            case MVP -> 16;
        };
        int silverCount = switch (tier) {
            case BOSS -> 1;
            case MVP -> 2;
            default -> 0;
        };
        int goldCount = tier == MobTier.MVP ? 1 : 0;

        // Roll for Copper
        if (random.nextDouble() < ZenyConfig.COPPER_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.COPPER_ZENY.get(), 1 + random.nextInt(copperMax)));
        }

        // Roll for Silver
        if (random.nextDouble() < ZenyConfig.SILVER_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.SILVER_ZENY.get(), Math.max(1, silverCount + 1)));
        }

        // Roll for Gold
        if (random.nextDouble() < ZenyConfig.GOLD_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.GOLD_ZENY.get(), Math.max(1, goldCount + 1)));
        }

        if (silverCount > 0) {
            drops.add(new ItemStack(ZenyItems.SILVER_ZENY.get(), silverCount));
        }

        if (goldCount > 0) {
            drops.add(new ItemStack(ZenyItems.GOLD_ZENY.get(), goldCount));
        }

        return drops;
    }

    private static double getDimensionMultiplier(ResourceLocation dim) {
        // Refresh cache every minute if config changes (simplified)
        if (System.currentTimeMillis() - lastConfigCheck > 60000) {
            DIM_MULT_CACHE.clear();
            List<? extends String> configList = ZenyConfig.DIMENSION_MULTIPLIERS.get();
            for (String s : configList) {
                String[] parts = s.split("=");
                if (parts.length == 2) {
                    try {
                        DIM_MULT_CACHE.put(new ResourceLocation(parts[0]), Double.parseDouble(parts[1]));
                    } catch (Exception ignored) {}
                }
            }
            lastConfigCheck = System.currentTimeMillis();
        }
        
        return Math.min(ZenyConfig.DIMENSION_MULT_CAP.get(), DIM_MULT_CACHE.getOrDefault(dim, 1.0D));
    }
}
