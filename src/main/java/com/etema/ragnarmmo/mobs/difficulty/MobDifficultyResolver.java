package com.etema.ragnarmmo.mobs.difficulty;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DifficultyRule;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DifficultyRules;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DimensionRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Optional;

public final class MobDifficultyResolver {
    public DifficultyResult resolve(DifficultyContext context) {
        DifficultyRules rules = MobConfigAccess.getDifficultyRules();
        DimensionRules dimension = rules.dimensions().getOrDefault(context.dimension(), rules.defaultDimension());
        if (!rules.enabled()) {
            return new DifficultyResult(1, MobRank.NORMAL, dimension.floor(), dimension.cap(),
                    context.structureId(), Optional.empty(), rules.mode());
        }

        RandomSource random = DeterministicMobRandom.from(context);
        int level = baseLevel(context, rules, dimension, random);

        level = clamp(level, dimension.floor(), dimension.cap());

        Optional<ResourceLocation> matchedStructure = context.structureId()
                .filter(id -> rules.structures().containsKey(id));
        if (matchedStructure.isPresent()) {
            DifficultyRule structureRule = rules.structures().get(matchedStructure.get());
            if (structureRule.minLevel().isPresent()) {
                level = Math.max(level, structureRule.minLevel().getAsInt());
            }
        }

        Optional<ResourceLocation> matchedBoss = Optional.ofNullable(context.entityType())
                .filter(id -> rules.bossRules().containsKey(id));
        if (matchedBoss.isPresent()) {
            DifficultyRule bossRule = rules.bossRules().get(matchedBoss.get());
            if (bossRule.minLevel().isPresent()) {
                level = Math.max(level, bossRule.minLevel().getAsInt());
            }
        }

        level = Math.min(level, rules.maxLevel());
        level = Math.max(1, level);

        MobRank rank = rules.rankChances().roll(random.nextDouble());
        if (matchedStructure.isPresent()) {
            rank = applyRankRule(rank, rules.structures().get(matchedStructure.get()), false);
        }
        if (matchedBoss.isPresent()) {
            rank = applyRankRule(rank, rules.bossRules().get(matchedBoss.get()), true);
        }

        return new DifficultyResult(level, rank, dimension.floor(), dimension.cap(), matchedStructure, matchedBoss,
                rules.mode());
    }

    private static int baseLevel(DifficultyContext context, DifficultyRules rules, DimensionRules dimension,
            RandomSource random) {
        int base = switch (rules.mode()) {
            case PLAYER_LEVEL -> context.nearestPlayerLevel().orElse(1);
            case STATIC -> 1;
            case REGION -> Math.max(1, (Math.abs(context.mobPos().getX()) + Math.abs(context.mobPos().getZ())) / 256);
            case DISTANCE -> distanceLevel(context, dimension);
        };

        if (rules.mode() == DifficultyMode.PLAYER_LEVEL && rules.playerLevelVariance() > 0) {
            int spread = rules.playerLevelVariance();
            base += random.nextInt(spread * 2 + 1) - spread;
        }
        return base;
    }

    private static int distanceLevel(DifficultyContext context, DimensionRules dimension) {
        int distance = (int) Math.floor(Math.sqrt(context.mobPos().distSqr(context.worldSpawnPos())));
        return dimension.rangeForDistance(distance)
                .map(range -> range.min() == range.max() ? range.min() : range.min() + (distance % (range.max() - range.min() + 1)))
                .orElse(Math.max(1, (distance / 125) + 1));
    }

    private static MobRank applyRankRule(MobRank current, DifficultyRule rule, boolean allowFixedRank) {
        if (allowFixedRank && rule.fixedRank().isPresent()) {
            return MobConfigAccess.maxSeverity(current, rule.fixedRank().get());
        }
        if (rule.minRank().isPresent()) {
            return MobConfigAccess.maxSeverity(current, rule.minRank().get());
        }
        return current;
    }

    private static int clamp(int value, int floor, int cap) {
        return Math.max(floor, Math.min(cap, value));
    }
}
