package com.etema.ragnarmmo.common.api.mobs.runtime;

import org.jetbrains.annotations.Nullable;

/**
 * Optional base combat attributes normalized into the new runtime profile.
 *
 * <p>This block is read-only runtime data derived from the manual path when those base attributes
 * are available safely. It is intentionally narrow and does not imply a broader combat redesign.</p>
 */
public record ComputedMobBaseStats(
        @Nullable Integer str,
        int vit,
        int intelligence,
        int agi,
        int luk,
        @Nullable Integer dex) {

    public ComputedMobBaseStats {
        if (str != null && str < 0) {
            throw new IllegalArgumentException("str must be >= 0 when present");
        }
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
        if (dex != null && dex < 0) {
            throw new IllegalArgumentException("dex must be >= 0 when present");
        }
    }
}
