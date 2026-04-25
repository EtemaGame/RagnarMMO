package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobProfileTest {
    @Test
    void acceptsValidProfile() {
        assertDoesNotThrow(() -> new MobProfile(1, MobRank.NORMAL, 20, 2, 4, 0, 0, 10, 5, 1, 150,
                0.2D, "unknown", "neutral", "medium"));
    }

    @Test
    void rejectsInvalidProfile() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(0, MobRank.NORMAL, 20, 2, 4, 0, 0, 10, 5, 1, 150,
                        0.2D, "unknown", "neutral", "medium"));
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(1, MobRank.NORMAL, 20, 5, 4, 0, 0, 10, 5, 1, 150,
                        0.2D, "unknown", "neutral", "medium"));
        assertThrows(IllegalArgumentException.class,
                () -> new MobProfile(1, MobRank.NORMAL, 20, 2, 4, 0, 0, 10, 5, 1, 0,
                        0.2D, "unknown", "neutral", "medium"));
    }
}
