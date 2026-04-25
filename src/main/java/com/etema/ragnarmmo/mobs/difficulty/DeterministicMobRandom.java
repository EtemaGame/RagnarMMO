package com.etema.ragnarmmo.mobs.difficulty;

import net.minecraft.util.RandomSource;

public final class DeterministicMobRandom {
    private DeterministicMobRandom() {
    }

    public static RandomSource from(DifficultyContext context) {
        long seed = context.worldSeed();
        seed = mix(seed, context.entityType().toString());
        seed = mix(seed, context.dimension().toString());
        seed = mix(seed, context.mobPos().asLong());
        return RandomSource.create(seed);
    }

    private static long mix(long seed, String value) {
        long mixed = seed;
        for (int i = 0; i < value.length(); i++) {
            mixed = mix(mixed, value.charAt(i));
        }
        return mixed;
    }

    private static long mix(long seed, long value) {
        long mixed = seed ^ value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
