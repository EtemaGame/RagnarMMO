package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

import java.util.Locale;

public final class BossRankRules {

    private BossRankRules() {
    }

    public static boolean shouldPersistWorldState(MobRank rank) {
        return rank == MobRank.MINI_BOSS || rank == MobRank.BOSS || rank == MobRank.MVP;
    }

    public static boolean isControlledSpawnRank(MobRank rank) {
        return shouldPersistWorldState(rank);
    }

    public static String displayName(MobRank rank) {
        if (rank == null) {
            return "Normal";
        }

        String raw = rank.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }
}
