package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobProfileTest {
    @Test
    void acceptsValidProfile() {
        assertDoesNotThrow(() -> new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 1, 3, 0, 0, 10, 5, 1, 150,
                0.2D, 1, 1, "unknown", "neutral", "medium"));
    }

    @Test
    void legacyConstructorDefaultsToNoviceBaseStats() {
        MobProfile profile = new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 1, 3, 0, 0, 10, 5, 1, 150,
                0.2D, 1, 1, "unknown", "neutral", "medium");

        assertEquals(RoBaseStats.novice(), profile.baseStats());
    }

    @Test
    void rejectsInvalidProfile() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(0, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 1, 3, 0, 0, 10, 5, 1, 150,
                        0.2D, 1, 1, "unknown", "neutral", "medium"));
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 5, 4, 1, 3, 0, 0, 10, 5, 1, 150,
                        0.2D, 1, 1, "unknown", "neutral", "medium"));
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 5, 3, 0, 0, 10, 5, 1, 150,
                        0.2D, 1, 1, "unknown", "neutral", "medium"));
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 1, 3, 0, 0, 10, 5, 1, 0,
                        0.2D, 1, 1, "unknown", "neutral", "medium"));
    }
}
