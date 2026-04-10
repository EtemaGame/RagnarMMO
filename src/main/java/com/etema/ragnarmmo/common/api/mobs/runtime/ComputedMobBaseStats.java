package com.etema.ragnarmmo.common.api.mobs.runtime;

/**
 * Optional base combat attributes normalized into the new runtime profile.
 *
 * <p>This block is read-only runtime data derived from the manual path when those base attributes
 * are available safely. It is intentionally narrow and does not imply a broader combat redesign.</p>
 */
public record ComputedMobBaseStats(
        int vit,
        int intelligence,
        int agi,
        int luk) {

    public ComputedMobBaseStats {
        if (vit < 0) {
            throw new IllegalArgumentException("vit must be >= 0");
        }
        if (intelligence < 0) {
            throw new IllegalArgumentException("intelligence must be >= 0");
        }
        if (agi < 0) {
            throw new IllegalArgumentException("agi must be >= 0");
        }
        if (luk < 0) {
            throw new IllegalArgumentException("luk must be >= 0");
        }
    }
}
