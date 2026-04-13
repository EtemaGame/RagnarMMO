package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimized access layer for Economy (Zeny) settings.
 * Caches complex parsed values like dimension multipliers.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EconomyConfigAccess {
    private static volatile Snapshot current = null;

    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == RagnarConfigs.SERVER_SPEC) {
            reload();
        }
    }

    public static void reload() {
        current = new Snapshot();
    }

    public static boolean isEnabled() { return RagnarConfigs.SERVER.zeny.enabled.get(); }
    public static int getBaseMobDrop() { return RagnarConfigs.SERVER.zeny.baseMobDrop.get(); }
    public static double getLevelMultiplier() { return RagnarConfigs.SERVER.zeny.levelMultiplier.get(); }
    public static double getEliteMultiplier() { return RagnarConfigs.SERVER.zeny.eliteMultiplier.get(); }
    public static double getBossMultiplier() { return RagnarConfigs.SERVER.zeny.bossMultiplier.get(); }
    public static double getLukBonusPerPoint() { return RagnarConfigs.SERVER.zeny.lukBonusPerPoint.get(); }
    public static long getMaxDropPerKill() { return RagnarConfigs.SERVER.zeny.maxDropPerKill.get(); }
    public static int getVillagerPriceMultiplier() { return RagnarConfigs.SERVER.zeny.villagerPriceMultiplier.get(); }

    // Drop Chances
    public static double getCopperBaseChance() { return RagnarConfigs.SERVER.zeny.copperBaseChance.get(); }
    public static double getSilverBaseChance() { return RagnarConfigs.SERVER.zeny.silverBaseChance.get(); }
    public static double getGoldBaseChance() { return RagnarConfigs.SERVER.zeny.goldBaseChance.get(); }
    public static double getEliteChanceMult() { return RagnarConfigs.SERVER.zeny.eliteChanceMult.get(); }
    public static double getBossChanceMult() { return RagnarConfigs.SERVER.zeny.bossChanceMult.get(); }
    public static double getDropLukBonusFactor() { return RagnarConfigs.SERVER.zeny.dropLukBonusFactor.get(); }

    public static double getDimensionMultiplier(ResourceLocation dimension) {
        Snapshot snap = snapshot();
        return Math.min(snap.dimensionMultipliers.getOrDefault(dimension, 1.0), snap.multiplierCap);
    }

    private static Snapshot snapshot() {
        Snapshot snap = current;
        if (snap == null) {
            synchronized (EconomyConfigAccess.class) {
                snap = current;
                if (snap == null) {
                    snap = new Snapshot();
                    current = snap;
                }
            }
        }
        return snap;
    }

    private static class Snapshot {
        final Map<ResourceLocation, Double> dimensionMultipliers = new HashMap<>();
        final double multiplierCap;

        Snapshot() {
            var zeny = RagnarConfigs.SERVER.zeny;
            this.multiplierCap = zeny.dimensionMultCap.get();

            for (String entry : zeny.dimensionMultipliers.get()) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        dimensionMultipliers.put(new ResourceLocation(parts[0].trim()), Double.parseDouble(parts[1].trim()));
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
