package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side cache for derived stats received from the server.
 * This avoids the need for local recalculation and ensures
 * the UI displays the same values the server is using.
 */
@OnlyIn(Dist.CLIENT)
public final class DerivedStatsClientCache {

    private static DerivedStats cached = null;
    private static long lastUpdateTime = 0;

    private DerivedStatsClientCache() {
    }

    /**
     * Updates the cached derived stats from server sync.
     */
    public static void update(DerivedStats stats) {
        cached = stats;
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Gets the cached derived stats, or null if not yet received.
     */
    public static DerivedStats get() {
        return cached;
    }

    /**
     * Returns true if we have cached stats.
     */
    public static boolean hasCache() {
        return cached != null;
    }

    /**
     * Gets the time of the last update in milliseconds.
     */
    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Clears the cache (e.g., on disconnect).
     */
    public static void clear() {
        cached = null;
        lastUpdateTime = 0;
    }
}
