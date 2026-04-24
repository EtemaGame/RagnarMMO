package com.etema.ragnarmmo.mobs.difficulty;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

public final class MobDifficultyResolver {
    public record Result(int level, MobRank rank) {
    }

    private final DifficultyMode mode;

    public MobDifficultyResolver(DifficultyMode mode) {
        this.mode = mode;
    }

    public Result resolve(DifficultyContext context) {
        int level = switch (mode) {
            case PLAYER_LEVEL -> context.nearestPlayerLevel().orElse(1);
            case STATIC -> 1;
            case REGION -> Math.max(1, (Math.abs(context.mobPos().getX()) + Math.abs(context.mobPos().getZ())) / 256);
            case DISTANCE -> Math.max(1, (int) Math.floor(
                    Math.sqrt(context.mobPos().distSqr(context.worldSpawnPos())) / 125.0D) + 1);
        };
        return new Result(level, MobRank.NORMAL);
    }
}
