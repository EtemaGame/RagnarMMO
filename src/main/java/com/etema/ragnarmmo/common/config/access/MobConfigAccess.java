package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobConfigAccess {
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

    public static boolean isEnabled() { return snapshot().enabled; }
    public static List<String> getMobExcludeList() { return snapshot().mobExcludeList; }
    public static int getMaxLevel() { return RagnarConfigs.SERVER.mobs.maxLevel.get(); }
    public static double getEliteChance() { return RagnarConfigs.SERVER.mobs.eliteChance.get(); }
    public static double getMiniBossChance() { return RagnarConfigs.SERVER.mobs.miniBossChance.get(); }
    public static double getBossChance() { return RagnarConfigs.SERVER.mobs.bossChance.get(); }
    public static double getMvpChance() { return RagnarConfigs.SERVER.mobs.mvpChance.get(); }
    public static double getNaturalTierChanceScale() { return RagnarConfigs.SERVER.mobs.naturalTierChanceScale.get(); }

    public static double getHpBase() { return RagnarConfigs.SERVER.mobs.hpBase.get(); }
    public static double getVitToHp() { return RagnarConfigs.SERVER.mobs.vitToHp.get(); }
    public static double getLevelToHp() { return RagnarConfigs.SERVER.mobs.levelToHp.get(); }
    public static double getAtkBase() { return RagnarConfigs.SERVER.mobs.atkBase.get(); }
    public static double getStrToAtk() { return RagnarConfigs.SERVER.mobs.strToAtk.get(); }
    public static double getDexToAtk() { return RagnarConfigs.SERVER.mobs.dexToAtk.get(); }
    public static double getLukToAtk() { return RagnarConfigs.SERVER.mobs.lukToAtk.get(); }
    public static double getArmorBase() { return RagnarConfigs.SERVER.mobs.armorBase.get(); }
    public static double getVitToArmor() { return RagnarConfigs.SERVER.mobs.vitToArmor.get(); }
    public static double getIntToArmor() { return RagnarConfigs.SERVER.mobs.intToArmor.get(); }
    public static double getAgiToSpeed() { return RagnarConfigs.SERVER.mobs.agiToSpeed.get(); }
    public static double getMaxMovementSpeed() { return RagnarConfigs.SERVER.mobs.maxMovementSpeed.get(); }
    public static double getLukToKbResist() { return RagnarConfigs.SERVER.mobs.lukToKbResist.get(); }

    public static double getDamagePerStr() { return RagnarConfigs.SERVER.mobs.damagePerStr.get(); }
    public static double getDamagePerDex() { return RagnarConfigs.SERVER.mobs.damagePerDex.get(); }
    public static double getReductionPerVit() { return RagnarConfigs.SERVER.mobs.reductionPerVit.get(); }

    public static int getBasePoints(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobs.basePointsNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobs.basePointsElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobs.basePointsMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobs.basePointsBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobs.basePointsMvp.get();
        };
    }

    public static int getPointsPerLevel(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobs.pointsPerLevelNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobs.pointsPerLevelElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobs.pointsPerLevelMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobs.pointsPerLevelBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobs.pointsPerLevelMvp.get();
        };
    }

    public static double getHealthMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobs.healthMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobs.healthMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobs.healthMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobs.healthMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobs.healthMultMvp.get();
        };
    }

    public static double getDamageMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobs.damageMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobs.damageMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobs.damageMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobs.damageMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobs.damageMultMvp.get();
        };
    }

    public static double getDefenseMultiplier(MobTier tier) {
        return switch (tier) {
            case NORMAL -> RagnarConfigs.SERVER.mobs.defenseMultNormal.get();
            case ELITE -> RagnarConfigs.SERVER.mobs.defenseMultElite.get();
            case MINI_BOSS -> RagnarConfigs.SERVER.mobs.defenseMultMiniBoss.get();
            case BOSS -> RagnarConfigs.SERVER.mobs.defenseMultBoss.get();
            case MVP -> RagnarConfigs.SERVER.mobs.defenseMultMvp.get();
        };
    }

    public static RagnarConfigs.LevelScalingMode getLevelScalingMode() {
        try { return RagnarConfigs.SERVER.mobs.levelScalingMode.get(); }
        catch (Exception e) { return RagnarConfigs.LevelScalingMode.DISTANCE; }
    }
    public static int getPlayerLevelRadius() {
        try { return RagnarConfigs.SERVER.mobs.playerLevelRadius.get(); }
        catch (Exception e) { return 64; }
    }
    public static int getPlayerLevelVariance() {
        try { return RagnarConfigs.SERVER.mobs.playerLevelVariance.get(); }
        catch (Exception e) { return 2; }
    }
    public static boolean renderNumericHealth() {
        try { return RagnarConfigs.SERVER.mobs.renderNumericHealth.get(); }
        catch (Exception e) { return true; }
    }

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

    public static double getPartyScalingRadius() { return RagnarConfigs.SERVER.mobs.partyScalingRadius.get(); }
    public static double getPartyHpMultiplier() { return RagnarConfigs.SERVER.mobs.partyHpMultiplier.get(); }
    public static double getPartyAtkMultiplier() { return RagnarConfigs.SERVER.mobs.partyAtkMultiplier.get(); }

    private static Snapshot snapshot() {
        Snapshot snap = current;
        if (snap == null) {
            synchronized (MobConfigAccess.class) {
                snap = current;
                if (snap == null) {
                    try {
                        snap = new Snapshot();
                    } catch (Exception e) {
                        snap = new Snapshot(true);
                    }
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
            var ms = RagnarConfigs.SERVER.mobs;
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

        Snapshot(boolean testMode) {
            enabled = true;
            mobExcludeList = List.of();
            overworldDefaults = null;
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
