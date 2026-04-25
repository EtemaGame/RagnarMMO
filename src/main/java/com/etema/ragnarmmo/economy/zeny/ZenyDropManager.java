package com.etema.ragnarmmo.economy.zeny;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadView;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.common.config.access.EconomyConfigAccess;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ZenyDropManager {

    public static List<ItemStack> calculateDrops(LivingEntity killed, Player killer, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        ZenyDropProfile dropProfile = resolveDropProfile(killed);
        
        // 2. Get Dimension Multiplier (Now handled via access layer snapshot)
        double dimMult = EconomyConfigAccess.getDimensionMultiplier(killed.level().dimension().location());
        
        // 3. Get Luck Bonus
        double luckBonus = killer.getCapability(PlayerStatsProvider.CAP).map(stats -> 
            1.0 + (stats.getLUK() * EconomyConfigAccess.getDropLukBonusFactor())).orElse(1.0);
        
        // 4. Calculate final multipliers
        double finalMult = dimMult * luckBonus * dropProfile.chanceMultiplier();

        // Roll for Copper
        if (random.nextDouble() < EconomyConfigAccess.getCopperBaseChance() * finalMult) {
            drops.add(new ItemStack(ZenyItems.COPPER_ZENY.get(), 1 + random.nextInt(dropProfile.copperMax())));
        }

        // Roll for Silver
        if (random.nextDouble() < EconomyConfigAccess.getSilverBaseChance() * finalMult) {
            drops.add(new ItemStack(ZenyItems.SILVER_ZENY.get(), Math.max(1, dropProfile.guaranteedSilver() + 1)));
        }

        // Roll for Gold
        if (random.nextDouble() < EconomyConfigAccess.getGoldBaseChance() * finalMult) {
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

        return switch (rank) {
            case ELITE -> new ZenyDropProfile(EconomyConfigAccess.getEliteDropMultiplier(), 5, 0, 0);
            case MINI_BOSS -> new ZenyDropProfile(EconomyConfigAccess.getMiniBossDropMultiplier(), 8, 1, 0);
            case BOSS -> new ZenyDropProfile(EconomyConfigAccess.getBossDropMultiplier(), 12, 2, 1);
            case NORMAL -> new ZenyDropProfile(1.0D, 2, 0, 0);
        };
    }

    private record ZenyDropProfile(
            double chanceMultiplier,
            int copperMax,
            int guaranteedSilver,
            int guaranteedGold) {
    }
}
