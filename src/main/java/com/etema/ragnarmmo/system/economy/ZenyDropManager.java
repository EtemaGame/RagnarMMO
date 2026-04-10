package com.etema.ragnarmmo.system.economy;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadView;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.roitems.ZenyItems;
import com.etema.ragnarmmo.system.economy.config.ZenyConfig;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
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
        ZenyDropProfile dropProfile = resolveDropProfile(killed);
        
        // 2. Get Dimension Multiplier
        double dimMult = getDimensionMultiplier(killed.level().dimension().location());
        
        // 3. Get Luck Bonus
        double luckBonus = killer.getCapability(PlayerStatsProvider.CAP).map(stats -> 1.0 + (stats.getLUK() * ZenyConfig.LUK_BONUS_FACTOR.get())).orElse(1.0);
        
        // 4. Calculate final multipliers
        double finalMult = dimMult * luckBonus * dropProfile.chanceMultiplier();

        // Roll for Copper
        if (random.nextDouble() < ZenyConfig.COPPER_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.COPPER_ZENY.get(), 1 + random.nextInt(dropProfile.copperMax())));
        }

        // Roll for Silver
        if (random.nextDouble() < ZenyConfig.SILVER_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.SILVER_ZENY.get(), Math.max(1, dropProfile.guaranteedSilver() + 1)));
        }

        // Roll for Gold
        if (random.nextDouble() < ZenyConfig.GOLD_BASE_CHANCE.get() * finalMult) {
            drops.add(new ItemStack(ZenyItems.GOLD_ZENY.get(), Math.max(1, dropProfile.guaranteedGold() + 1)));
        }

        if (dropProfile.guaranteedSilver() > 0) {
            drops.add(new ItemStack(ZenyItems.SILVER_ZENY.get(), dropProfile.guaranteedSilver()));
        }

        if (dropProfile.guaranteedGold() > 0) {
            drops.add(new ItemStack(ZenyItems.GOLD_ZENY.get(), dropProfile.guaranteedGold()));
        }

        return drops;
    }

    private static ZenyDropProfile resolveDropProfile(LivingEntity killed) {
        MobConsumerReadView readView = MobConsumerReadViewResolver
                .resolve(killed)
                .orElse(null);
        MobRank rank = readView != null ? readView.rank() : MobRank.NORMAL;

        // Economy now reads semantic rank from the shared read surface. Any legacy-only
        // distinctions not carried by rank collapse conservatively to the lower ELITE/BOSS profile.
        return switch (rank) {
            case ELITE -> new ZenyDropProfile(ZenyConfig.ELITE_CHANCE_MULT.get(), 5, 0, 0);
            case BOSS -> new ZenyDropProfile(ZenyConfig.BOSS_CHANCE_MULT.get(), 12, 1, 0);
            case NORMAL -> new ZenyDropProfile(1.0D, 2, 0, 0);
        };
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

    private record ZenyDropProfile(
            double chanceMultiplier,
            int copperMax,
            int guaranteedSilver,
            int guaranteedGold) {
    }
}
