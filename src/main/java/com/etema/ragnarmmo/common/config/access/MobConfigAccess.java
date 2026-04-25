package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobConfigAccess {
    private static volatile Snapshot current = null;

    private MobConfigAccess() {
    }

    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == RagnarConfigs.SERVER_SPEC) {
            reload();
        }
    }

    public static void reload() {
        current = new Snapshot();
    }

    public static boolean isEnabled() {
        return snapshot().enabled;
    }

    public static List<String> getMobExcludeList() {
        return snapshot().excludeList;
    }

    public static boolean isExcluded(ResourceLocation entityType) {
        return entityType != null && snapshot().excludeList.contains(entityType.toString());
    }

    public static boolean renderNumericHealth() {
        return snapshot().renderNumericHealth;
    }

    public static DefaultProfile getDefaultProfile() {
        return snapshot().defaultProfile;
    }

    public static FormulaRules getFormulaRules() {
        return snapshot().formulaRules;
    }

    public static DifficultyRules getDifficultyRules() {
        return snapshot().difficultyRules;
    }

    public static double getMaxMovementSpeed() {
        return snapshot().formulaRules.moveSpeedCap();
    }

    public static double getDamagePerStr() {
        return RagnarConfigs.SERVER.combat.mobDamagePerStr.get();
    }

    public static double getDamagePerDex() {
        return RagnarConfigs.SERVER.combat.mobDamagePerDex.get();
    }

    public static double getReductionPerVit() {
        return RagnarConfigs.SERVER.combat.mobReductionPerVit.get();
    }

    public static double getPartyScalingRadius() {
        return RagnarConfigs.SERVER.mobs.partyScalingRadius.get();
    }

    public static double getPartyHpMultiplier() {
        return RagnarConfigs.SERVER.mobs.partyHpMultiplier.get();
    }

    public static double getPartyAtkMultiplier() {
        return RagnarConfigs.SERVER.mobs.partyAtkMultiplier.get();
    }

    private static Snapshot snapshot() {
        Snapshot snap = current;
        if (snap == null) {
            synchronized (MobConfigAccess.class) {
                snap = current;
                if (snap == null) {
                    snap = new Snapshot();
                    current = snap;
                }
            }
        }
        return snap;
    }

    private static final class Snapshot {
        final boolean enabled;
        final List<String> excludeList;
        final boolean renderNumericHealth;
        final DefaultProfile defaultProfile;
        final FormulaRules formulaRules;
        final DifficultyRules difficultyRules;

        Snapshot() {
            var mobs = RagnarConfigs.SERVER.mobs;
            var difficulty = RagnarConfigs.SERVER.difficulty;
            enabled = mobs.enabled.get();
            excludeList = List.copyOf(mobs.excludeList.get());
            renderNumericHealth = mobs.renderNumericHealth.get();
            defaultProfile = readDefaultProfile(mobs.defaultProfile);
            formulaRules = readFormulaRules(mobs.attributes);
            difficultyRules = readDifficultyRules(difficulty);
        }
    }

    private static DefaultProfile readDefaultProfile(RagnarConfigs.Server.Mobs.DefaultProfile config) {
        return new DefaultProfile(
                token(config.race.get(), "race"),
                token(config.element.get(), "element"),
                token(config.size.get(), "size"),
                config.maxHp.get(),
                config.atkMin.get(),
                config.atkMax.get(),
                config.def.get(),
                config.mdef.get(),
                config.hit.get(),
                config.flee.get(),
                config.crit.get(),
                config.aspd.get(),
                config.moveSpeed.get());
    }

    private static FormulaRules readFormulaRules(RagnarConfigs.Server.Mobs.Attributes config) {
        return new FormulaRules(
                config.hpPerLevel.get(),
                config.atkMinPerLevel.get(),
                config.atkMaxExtraPerLevel.get(),
                config.defPerLevel.get(),
                config.mdefPerLevel.get(),
                config.hitPerLevel.get(),
                config.fleePerLevel.get(),
                config.aspdPerLevel.get(),
                config.moveSpeedPerLevel.get(),
                config.moveSpeedCap.get());
    }

    private static DifficultyRules readDifficultyRules(RagnarConfigs.Server.Difficulty config) {
        RankChanceTable rankChances = new RankChanceTable(
                config.rankChances.elite.get(),
                config.rankChances.miniBoss.get(),
                config.rankChances.boss.get(),
                config.rankChances.mvp.get());

        Map<ResourceLocation, DimensionRules> dimensions = new HashMap<>();
        dimensions.put(Level.OVERWORLD.location(), new DimensionRules(config.overworld));
        dimensions.put(Level.NETHER.location(), new DimensionRules(config.nether));
        dimensions.put(Level.END.location(), new DimensionRules(config.end));

        return new DifficultyRules(
                config.enabled.get(),
                config.mode.get(),
                config.maxLevel.get(),
                rankChances,
                config.playerLevel.radius.get(),
                config.playerLevel.variance.get(),
                Map.copyOf(dimensions),
                dimensions.get(Level.OVERWORLD.location()),
                parseRuleMap(config.structures.get(), false),
                parseRuleMap(config.bossRules.get(), true));
    }

    private static Map<ResourceLocation, DifficultyRule> parseRuleMap(Map<String, String> raw, boolean bossRule) {
        Map<ResourceLocation, DifficultyRule> parsed = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            ResourceLocation id = new ResourceLocation(entry.getKey().trim());
            DifficultyRule rule = DifficultyRule.parse(entry.getValue(), bossRule);
            parsed.put(id, rule);
        }
        return Map.copyOf(parsed);
    }

    private static String token(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("mobs.default_profile." + field + " must not be blank");
        }
        return value.trim();
    }

    public record DefaultProfile(
            String race,
            String element,
            String size,
            int maxHp,
            int atkMin,
            int atkMax,
            int def,
            int mdef,
            int hit,
            int flee,
            int crit,
            int aspd,
            double moveSpeed) {
        public DefaultProfile {
            if (maxHp <= 0) throw new IllegalArgumentException("default maxHp must be > 0");
            if (atkMin < 0) throw new IllegalArgumentException("default atkMin must be >= 0");
            if (atkMax < atkMin) throw new IllegalArgumentException("default atkMax must be >= atkMin");
            if (aspd <= 0) throw new IllegalArgumentException("default aspd must be > 0");
            if (moveSpeed <= 0.0D) throw new IllegalArgumentException("default moveSpeed must be > 0");
        }
    }

    public record FormulaRules(
            double hpPerLevel,
            double atkMinPerLevel,
            double atkMaxExtraPerLevel,
            double defPerLevel,
            double mdefPerLevel,
            double hitPerLevel,
            double fleePerLevel,
            double aspdPerLevel,
            double moveSpeedPerLevel,
            double moveSpeedCap) {
        public FormulaRules {
            if (moveSpeedCap <= 0.0D) throw new IllegalArgumentException("moveSpeedCap must be > 0");
        }
    }

    public record DifficultyRules(
            boolean enabled,
            DifficultyMode mode,
            int maxLevel,
            RankChanceTable rankChances,
            int playerLevelRadius,
            int playerLevelVariance,
            Map<ResourceLocation, DimensionRules> dimensions,
            DimensionRules defaultDimension,
            Map<ResourceLocation, DifficultyRule> structures,
            Map<ResourceLocation, DifficultyRule> bossRules) {
    }

    public record RankChanceTable(double elite, double miniBoss, double boss, double mvp) {
        public RankChanceTable {
            if (elite < 0.0D || miniBoss < 0.0D || boss < 0.0D || mvp < 0.0D) {
                throw new IllegalArgumentException("difficulty.rank_chances must be >= 0");
            }
            double sum = elite + miniBoss + boss + mvp;
            if (sum > 1.0D) {
                throw new IllegalArgumentException("difficulty.rank_chances sum must not exceed 1.0");
            }
        }

        public MobRank roll(double roll) {
            double cursor = mvp;
            if (roll < cursor) return MobRank.MVP;
            cursor += boss;
            if (roll < cursor) return MobRank.BOSS;
            cursor += miniBoss;
            if (roll < cursor) return MobRank.MINI_BOSS;
            cursor += elite;
            if (roll < cursor) return MobRank.ELITE;
            return MobRank.NORMAL;
        }
    }

    public static final class DimensionRules {
        private final int floor;
        private final int cap;
        private final List<DistanceBand> distanceBands;

        DimensionRules(RagnarConfigs.DimensionConfig config) {
            this.floor = config.minFloor.get();
            this.cap = config.maxCap.get();
            if (floor > cap) {
                throw new IllegalArgumentException("dimension floor must be <= cap");
            }
            List<DistanceBand> bands = new ArrayList<>();
            for (String value : config.distanceBands.get()) {
                bands.add(DistanceBand.parse(value));
            }
            bands.sort(java.util.Comparator.comparingInt(DistanceBand::minDistance));
            this.distanceBands = List.copyOf(bands);
        }

        public int floor() {
            return floor;
        }

        public int cap() {
            return cap;
        }

        public Optional<IntRange> rangeForDistance(int distance) {
            for (DistanceBand band : distanceBands) {
                if (distance >= band.minDistance() && distance <= band.maxDistance()) {
                    return Optional.of(band.levelRange());
                }
            }
            return Optional.empty();
        }
    }

    public record DifficultyRule(OptionalInt minLevel, Optional<MobRank> minRank, Optional<MobRank> fixedRank) {
        public static DifficultyRule parse(String raw, boolean bossRule) {
            OptionalInt minLevel = OptionalInt.empty();
            Optional<MobRank> minRank = Optional.empty();
            Optional<MobRank> fixedRank = Optional.empty();
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("difficulty rule must not be blank");
            }
            for (String token : raw.split(",")) {
                String[] parts = token.trim().split("=", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("invalid difficulty rule token: " + token);
                }
                String key = parts[0].trim().toLowerCase(Locale.ROOT);
                String value = parts[1].trim();
                switch (key) {
                    case "min_level" -> minLevel = OptionalInt.of(Integer.parseInt(value));
                    case "min_rank" -> minRank = Optional.of(MobRank.valueOf(value.toUpperCase(Locale.ROOT)));
                    case "rank" -> fixedRank = Optional.of(MobRank.valueOf(value.toUpperCase(Locale.ROOT)));
                    default -> throw new IllegalArgumentException("unknown difficulty rule key: " + key);
                }
            }
            if (bossRule && minLevel.isEmpty() && minRank.isEmpty() && fixedRank.isEmpty()) {
                throw new IllegalArgumentException("boss rule must define min_level, min_rank, or rank");
            }
            return new DifficultyRule(minLevel, minRank, fixedRank);
        }
    }

    public record IntRange(int min, int max) {
        public IntRange {
            if (min < 1 || max < min) {
                throw new IllegalArgumentException("invalid level range " + min + "-" + max);
            }
        }

        public static IntRange parse(String raw) {
            String value = raw.trim();
            String[] parts = value.split("-", 2);
            if (parts.length == 1) {
                int exact = Integer.parseInt(value);
                return new IntRange(exact, exact);
            }
            return new IntRange(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }
    }

    public record DistanceBand(int minDistance, int maxDistance, IntRange levelRange) {
        public DistanceBand {
            if (minDistance < 0 || maxDistance < minDistance) {
                throw new IllegalArgumentException("invalid distance band");
            }
        }

        public static DistanceBand parse(String raw) {
            String[] parts = raw.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid distance band: " + raw);
            }
            String distancePart = parts[0].trim();
            IntRange levels = IntRange.parse(parts[1].trim());
            if (distancePart.endsWith("+")) {
                int min = Integer.parseInt(distancePart.substring(0, distancePart.length() - 1).trim());
                return new DistanceBand(min, Integer.MAX_VALUE, levels);
            }
            String[] distanceParts = distancePart.split("-", 2);
            if (distanceParts.length == 1) {
                int exact = Integer.parseInt(distancePart);
                return new DistanceBand(exact, exact, levels);
            }
            return new DistanceBand(
                    Integer.parseInt(distanceParts[0].trim()),
                    Integer.parseInt(distanceParts[1].trim()),
                    levels);
        }
    }

    public static MobRank maxSeverity(MobRank left, MobRank right) {
        if (left == null) return right == null ? MobRank.NORMAL : right;
        if (right == null) return left;
        return severity(left) >= severity(right) ? left : right;
    }

    private static int severity(MobRank rank) {
        return switch (rank) {
            case NORMAL -> 0;
            case ELITE -> 1;
            case MINI_BOSS -> 2;
            case BOSS -> 3;
            case MVP -> 4;
        };
    }
}
