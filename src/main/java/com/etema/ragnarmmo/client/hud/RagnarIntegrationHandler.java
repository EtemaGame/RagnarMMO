package com.etema.ragnarmmo.client.hud;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Integration resolver for the unified RagnarMMO jar.
 *
 * Since RagnarMMO bundles all modules, we don't use ModList checks here
 * (those would only make sense for separate standalone jars).
 */
public final class RagnarIntegrationHandler {
    public static boolean hasRagnarStats;
    public static boolean hasMobResolver;

    private RagnarIntegrationHandler() {}

    public static void init() {
        hasRagnarStats = true;
        hasMobResolver = true;
    }

    public static EntityStatResolver getResolverFor(LivingEntity e) {
        if (hasMobResolver && !(e instanceof Player)) return new MobStatsIntegration();
        if (hasRagnarStats && e instanceof Player) return new RagnarStatsIntegration();
        return null;
    }
}






