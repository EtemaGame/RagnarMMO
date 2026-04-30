package com.etema.ragnarmmo.common.api.stats;

/**
 * Shared Ragnarok Online base stat block for non-player combatants.
 */
public record RoBaseStats(int str, int agi, int vit, int intel, int dex, int luk) {
    public RoBaseStats {
        str = positive(str);
        agi = positive(agi);
        vit = positive(vit);
        intel = positive(intel);
        dex = positive(dex);
        luk = positive(luk);
    }

    public static RoBaseStats novice() {
        return new RoBaseStats(1, 1, 1, 1, 1, 1);
    }

    private static int positive(int value) {
        return Math.max(1, value);
    }
}
