package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * High-performance access layer for Mob Statistics and Scaling.
 * Parses complex bands and rules into immutable snapshots on reload.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobStatsConfigAccess {
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

    // --- Basic Getters ---
    public static boolean isEnabled() { return snapshot().enabled; }
    public static List<String> getMobExcludeList() { return snapshot().mobExcludeList; }
    public static int getMaxLevel() { return RagnarConfigs.SERVER.mobstats.maxLevel.get(); }
    public static double getEliteChance() { return RagnarConfigs.SERVER.mobstats.eliteChance.get(); }
    public static double getMiniBossChance() { return RagnarConfigs.SERVER.mobstats.miniBossChance.get(); }
    public static double getBossChance() { return RagnarConfigs.SERVER.mobstats.bossChance.get(); }
    public static double getMvpChance() { return RagnarConfigs.SERVER.mobstats.mvpChance.get(); }
    public static double getNaturalTierChanceScale() { return RagnarConfigs.SERVER.mobstats.naturalTierChanceScale.get(); }

    // --- Attribute Getters ---
    public static double getHpBase() { return RagnarConfigs.SERVER.mobstats.hpBase.get(); }
    public static double getVitToHp() { return RagnarConfigs.SERVER.mobstats.vitToHp.get(); }
    public static double getLevelToHp() { return RagnarConfigs.SERVER.mobstats.levelToHp.get(); }
    public static double getAtkBase() { return RagnarConfigs.SERVER.mobstats.atkBase.get(); }
    public static double getStrToAtk() { return RagnarConfigs.SERVER.mobstats.strToAtk.get(); }
    public static double getDexToAtk() { return RagnarConfigs.SERVER.mobstats.dexToAtk.get(); }
    public static double getLukToAtk() { return RagnarConfigs.SERVER.mobstats.lukToAtk.get(); }
    public static double getArmorBase() { return RagnarConfigs.SERVER.mobstats.armorBase.get(); }
    public static double getVitToArmor() { return RagnarConfigs.SERVER.mobstats.vitToArmor.get(); }
    public static double getIntToArmor() { return RagnarConfigs.SERVER.mobstats.intToArmor.get(); }
    public static double getAgiToSpeed() { return RagnarConfigs.SERVER.mobstats.agiToSpeed.get(); }
    public static double getMaxMovementSpeed() { return RagnarConfigs.SERVER.mobstats.maxMovementSpeed.get(); }
    public static double getLukToKbResist() { return RagnarConfigs.SERVER.mobstats.lukToKbResist.get(); }

    // --- Combat Getters ---
    public static double getDamagePerStr() { return RagnarConfigs.SERVER.mobstats.damagePerStr.get(); }
    public static double getDamagePerDex() { return RagnarConfigs.SERVER.mobstats.damagePerDex.get(); }
    public static double getReductionPerVit() { return RagnarConfigs.SERVER.mobstats.reductionPerVit.get(); }

    // --- Tier Helpers ---
    public static int getBasePoints(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobstats.basePointsNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobstats.basePointsElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobstats.basePointsMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobstats.basePointsBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobstats.basePointsMvp.get();
        };
    }

    public static int getPointsPerLevel(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobstats.pointsPerLevelNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobstats.pointsPerLevelElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobstats.pointsPerLevelMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobstats.pointsPerLevelBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobstats.pointsPerLevelMvp.get();
        };
    }

    public static double getHealthMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobstats.healthMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobstats.healthMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobstats.healthMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobstats.healthMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobstats.healthMultMvp.get();
        };
    }

    public static double getDamageMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobstats.damageMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobstats.damageMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobstats.damageMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobstats.damageMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobstats.damageMultMvp.get();
        };
    }

    public static double getDefenseMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobstats.defenseMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobstats.defenseMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobstats.defenseMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobstats.defenseMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobstats.defenseMultMvp.get();
        };
    }

    // --- World Scaling ---
    public static RagnarConfigs.LevelScalingMode getLevelScalingMode() { return RagnarConfigs.SERVER.mobstats.levelScalingMode.get(); }
    public static RagnarConfigs.ManualMobBackend getManualMobBackend() { return RagnarConfigs.SERVER.mobstats.manualMobBackend.get(); }
    public static RagnarConfigs.ManualUncoveredBehavior getManualUncoveredBehavior() { return RagnarConfigs.SERVER.mobstats.manualUncoveredBehavior.get(); }
    public static RagnarConfigs.AutomaticFallbackMode getManualFallbackAutomaticMode() { return RagnarConfigs.SERVER.mobstats.manualFallbackAutomaticMode.get(); }
    public static boolean isManualMobEditorEnabled() { return RagnarConfigs.SERVER.mobstats.enableManualMobEditor.get(); }
    public static boolean isManualMobDiscoveryEnabled() { return RagnarConfigs.SERVER.mobstats.enableManualMobDiscovery.get(); }
    public static int getPlayerLevelRadius() { return RagnarConfigs.SERVER.mobstats.playerLevelRadius.get(); }
    public static int getPlayerLevelVariance() { return RagnarConfigs.SERVER.mobstats.playerLevelVariance.get(); }
    public static boolean renderNumericHealth() { return RagnarConfigs.SERVER.mobstats.renderNumericHealth.get(); }

    public static ParsedDimensionRules getDimensionRules(ResourceKey<Level> dimension) {
        return snapshot().dimensionRules.getOrDefault(dimension.location(), snapshot().overworldDefaults);
    }

    public static int getDimensionMinLevel(ResourceLocation dimension) {
        return snapshot().dimensionMinLevels.getOrDefault(dimension, 1);
    }

    public static int getStructureMinLevel(ResourceLocation structure) {
        return snapshot().structureMinLevels.getOrDefault(structure, 1);
    }

    public static int getBossMinLevel(ResourceLocation mobId) {
        return snapshot().bossMinLevels.getOrDefault(mobId, 1);
    }

    public static double getPartyScalingRadius() { return RagnarConfigs.SERVER.mobstats.partyScalingRadius.get(); }
    public static double getPartyHpMultiplier() { return RagnarConfigs.SERVER.mobstats.partyHpMultiplier.get(); }
    public static double getPartyAtkMultiplier() { return RagnarConfigs.SERVER.mobstats.partyAtkMultiplier.get(); }

    // --- Snapshot Classes ---

    private static Snapshot snapshot() {
        Snapshot snap = current;
        if (snap == null) {
            synchronized (MobStatsConfigAccess.class) {
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
        final Map<ResourceLocation, ParsedDimensionRules> dimensionRules = new HashMap<>();
        final Map<ResourceLocation, Integer> dimensionMinLevels = new HashMap<>();
        final Map<ResourceLocation, Integer> structureMinLevels = new HashMap<>();
        final Map<ResourceLocation, Integer> bossMinLevels = new HashMap<>();
        final List<String> mobExcludeList;
        final boolean enabled;
        final ParsedDimensionRules overworldDefaults;

        Snapshot() {
            var ms = RagnarConfigs.SERVER.mobstats;
            enabled = ms.enabled.get();
            mobExcludeList = List.copyOf(ms.mobExcludeList.get());
            overworldDefaults = new ParsedDimensionRules(ms.overworld);
            
            dimensionRules.put(Level.OVERWORLD.location(), overworldDefaults);
            dimensionRules.put(Level.NETHER.location(), new ParsedDimensionRules(ms.nether));
            dimensionRules.put(Level.END.location(), new ParsedDimensionRules(ms.end));

            parseMap(ms.dimensionMinLevels.get(), dimensionMinLevels);
            parseMap(ms.structureMinLevels.get(), structureMinLevels);
            parseMap(ms.bossMinLevels.get(), bossMinLevels);
        }

        private void parseMap(List<? extends String> list, Map<ResourceLocation, Integer> target) {
            for (String s : list) {
                String[] p = s.split("=");
                if (p.length == 2) {
                    try {
                        target.put(new ResourceLocation(p[0].trim()), Integer.parseInt(p[1].trim()));
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public static class ParsedDimensionRules {
        public final int minFloor;
        public final int maxCap;
        public final List<DistanceBand> distanceBands = new ArrayList<>();

        public ParsedDimensionRules(RagnarConfigs.DimensionConfig config) {
            this.minFloor = config.minFloor.get();
            this.maxCap = config.maxCap.get();

            for (String s : config.distanceBands.get()) {
                DistanceBand b = DistanceBand.parse(s);
                if (b != null) distanceBands.add(b);
            }
            
            distanceBands.sort(Comparator.comparingInt(b -> b.minDistance));
        }
    }

    public record IntRange(int min, int max) {
        public static IntRange parse(String s) {
            String[] p = s.split("-");
            if (p.length == 2) return new IntRange(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            return new IntRange(Integer.parseInt(s), Integer.parseInt(s));
        }
    }

    public static class DistanceBand {
        public final int minDistance;
        public final int maxDistance;
        public final IntRange levelRange;

        public DistanceBand(int min, int max, IntRange levels) {
            this.minDistance = min;
            this.maxDistance = max;
            this.levelRange = levels;
        }

        public static DistanceBand parse(String s) {
            try {
                String[] p = s.split("=");
                if (p.length != 2) return null;
                IntRange levels = IntRange.parse(p[1]);
                String distPart = p[0].trim();
                if (distPart.endsWith("+")) {
                    int min = Integer.parseInt(distPart.substring(0, distPart.length()-1));
                    return new DistanceBand(min, Integer.MAX_VALUE, levels);
                }
                IntRange dists = IntRange.parse(distPart);
                return new DistanceBand(dists.min, dists.max, levels);
            } catch (Exception e) { return null; }
        }
    }

}
