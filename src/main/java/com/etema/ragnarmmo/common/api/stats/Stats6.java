package com.etema.ragnarmmo.common.api.stats;

/**
 * A simple record to hold the 6 primary stats.
 * Used for Job Bonuses and other fixed stat modifications.
 */
public record Stats6(int str, int agi, int vit, int int_, int dex, int luk) {
    public static final Stats6 ZERO = new Stats6(0, 0, 0, 0, 0, 0);

    public Stats6 add(Stats6 other) {
        return new Stats6(
                this.str + other.str,
                this.agi + other.agi,
                this.vit + other.vit,
                this.int_ + other.int_,
                this.dex + other.dex,
                this.luk + other.luk);
    }
}
