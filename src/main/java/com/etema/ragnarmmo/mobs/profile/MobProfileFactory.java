package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DefaultProfile;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.FormulaRules;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyResult;

import java.util.Optional;

public final class MobProfileFactory {
    private final DefaultProfile defaultProfileOverride;
    private final FormulaRules formulaRulesOverride;

    public MobProfileFactory() {
        this(null, null);
    }

    MobProfileFactory(DefaultProfile defaultProfileOverride, FormulaRules formulaRulesOverride) {
        this.defaultProfileOverride = defaultProfileOverride;
        this.formulaRulesOverride = formulaRulesOverride;
    }

    public MobProfile create(DifficultyResult difficulty, Optional<AuthoredMobDefinition> authoredDefinition) {
        DefaultProfile defaults = defaultProfileOverride != null ? defaultProfileOverride : MobConfigAccess.getDefaultProfile();
        FormulaRules formulas = formulaRulesOverride != null ? formulaRulesOverride : MobConfigAccess.getFormulaRules();
        AuthoredMobDefinition authored = authoredDefinition.orElse(null);
        int level = difficulty.level();
        MobTier proceduralTier = MobTier.fromRank(difficulty.rank());
        MobTier tier = authored == null ? proceduralTier : maxTier(proceduralTier, authored.tier().orElse(proceduralTier));
        TierModifiers modifiers = TierModifiers.forTier(tier);

        int baseLevel = authored == null ? level : authored.baseLevel().orElse(level);
        boolean authoredBaseline = authored != null && authored.baseLevel().isPresent();
        int maxHp = authoredScaledInt(authored, AuthoredMobDefinition::baseHp, level, baseLevel, 1.45D, modifiers.hp(),
                modifiers.applyInt(defaults.maxHp() + scaled(level, formulas.hpPerLevel()), TierStat.HP));
        int atkMin = authoredScaledInt(authored, AuthoredMobDefinition::atkMin, level, baseLevel, 1.15D, modifiers.atk(),
                modifiers.applyInt(defaults.atkMin() + scaled(level, formulas.atkMinPerLevel()), TierStat.ATK));
        int atkMax = Math.max(atkMin, authoredScaledInt(authored, AuthoredMobDefinition::atkMax, level, baseLevel, 1.15D, modifiers.atk(),
                Math.max(atkMin, modifiers.applyInt(defaults.atkMax() + scaled(level, formulas.atkMinPerLevel() + formulas.atkMaxExtraPerLevel()), TierStat.ATK))));
        int matkMin = authoredScaledInt(authored, AuthoredMobDefinition::matkMin, level, baseLevel, 1.15D, modifiers.atk(),
                Math.max(0, (int) Math.round((defaults.atkMin() + scaled(level, formulas.atkMinPerLevel())) * 0.75D)));
        int matkMax = Math.max(matkMin, authoredScaledInt(authored, AuthoredMobDefinition::matkMax, level, baseLevel, 1.15D, modifiers.atk(),
                Math.max(matkMin, (int) Math.round((defaults.atkMax() + scaled(level, formulas.atkMinPerLevel() + formulas.atkMaxExtraPerLevel())) * 0.85D))));
        int def = authoredScaledInt(authored, AuthoredMobDefinition::def, level, baseLevel, 1.10D, modifiers.def(), modifiers.applyInt(defaults.def() + scaled(level, formulas.defPerLevel()), TierStat.DEF));
        int mdef = authoredScaledInt(authored, AuthoredMobDefinition::mdef, level, baseLevel, 1.10D, modifiers.def(), modifiers.applyInt(defaults.mdef() + scaled(level, formulas.mdefPerLevel()), TierStat.DEF));
        int hit = authoredLinearInt(authored, AuthoredMobDefinition::hit, level, baseLevel, formulas.hitPerLevel(), modifiers.accuracy(), modifiers.applyInt(defaults.hit() + scaled(level, formulas.hitPerLevel()), TierStat.ACCURACY));
        int flee = authoredLinearInt(authored, AuthoredMobDefinition::flee, level, baseLevel, formulas.fleePerLevel(), modifiers.accuracy(), modifiers.applyInt(defaults.flee() + scaled(level, formulas.fleePerLevel()), TierStat.ACCURACY));
        int crit = authoredLinearInt(authored, AuthoredMobDefinition::crit, level, baseLevel, 0.05D, modifiers.crit(), modifiers.applyInt(defaults.crit(), TierStat.CRIT));
        int aspd = authoredLinearInt(authored, AuthoredMobDefinition::aspd, level, baseLevel, formulas.aspdPerLevel(), modifiers.aspd(), modifiers.applyInt(defaults.aspd() + scaled(level, formulas.aspdPerLevel()), TierStat.ASPD));
        double moveSpeed = authoredDouble(authored, AuthoredMobDefinition::moveSpeed,
                Math.min(formulas.moveSpeedCap(), defaults.moveSpeed() + (level * formulas.moveSpeedPerLevel())));
        int baseExp = authoredBaseline && authored.baseExp().isPresent() && level == baseLevel
                ? authored.baseExp().getAsInt()
                : MobRewardFormula.baseExp(level, tier);
        int jobExp = authoredBaseline && authored.jobExp().isPresent() && level == baseLevel
                ? authored.jobExp().getAsInt()
                : MobRewardFormula.jobExp(level, tier);

        String race = authoredString(authored, AuthoredMobDefinition::race, defaults.race());
        String element = authoredString(authored, AuthoredMobDefinition::element, defaults.element());
        String size = authoredString(authored, AuthoredMobDefinition::size, defaults.size());

        return new MobProfile(level, difficulty.rank(), tier, maxHp, atkMin, atkMax, matkMin, matkMax, def, mdef, hit, flee, crit, aspd,
                moveSpeed, baseExp, jobExp, race, element, size);
    }

    private static int scaled(int level, double perLevel) {
        return (int) Math.round(Math.max(0, level) * perLevel);
    }

    private static int authoredInt(AuthoredMobDefinition authored,
            java.util.function.Function<AuthoredMobDefinition, java.util.OptionalInt> getter,
            int defaultValue) {
        if (authored == null) {
            return defaultValue;
        }
        java.util.OptionalInt value = getter.apply(authored);
        return value.isPresent() ? value.getAsInt() : defaultValue;
    }

    private static int authoredScaledInt(AuthoredMobDefinition authored,
            java.util.function.Function<AuthoredMobDefinition, java.util.OptionalInt> getter,
            int runtimeLevel,
            int baseLevel,
            double exponent,
            double tierMultiplier,
            int defaultValue) {
        if (authored == null) {
            return defaultValue;
        }
        java.util.OptionalInt value = getter.apply(authored);
        if (value.isEmpty()) {
            return defaultValue;
        }
        if (runtimeLevel == baseLevel) {
            return value.getAsInt();
        }
        double ratio = Math.max(1.0D, runtimeLevel) / (double) Math.max(1, baseLevel);
        return Math.max(0, (int) Math.round(value.getAsInt() * Math.pow(ratio, exponent) * tierMultiplier));
    }

    private static int authoredLinearInt(AuthoredMobDefinition authored,
            java.util.function.Function<AuthoredMobDefinition, java.util.OptionalInt> getter,
            int runtimeLevel,
            int baseLevel,
            double perLevel,
            double tierMultiplier,
            int defaultValue) {
        if (authored == null) {
            return defaultValue;
        }
        java.util.OptionalInt value = getter.apply(authored);
        if (value.isEmpty()) {
            return defaultValue;
        }
        if (runtimeLevel == baseLevel) {
            return value.getAsInt();
        }
        int delta = runtimeLevel - baseLevel;
        return Math.max(0, (int) Math.round(value.getAsInt() + (delta * perLevel * tierMultiplier)));
    }

    private static MobTier maxTier(MobTier first, MobTier second) {
        return severity(first) >= severity(second) ? first : second;
    }

    private static int severity(MobTier tier) {
        return switch (tier == null ? MobTier.NORMAL : tier) {
            case WEAK -> 0;
            case NORMAL -> 1;
            case ELITE -> 2;
            case BOSS -> 3;
        };
    }

    private static double authoredDouble(AuthoredMobDefinition authored,
            java.util.function.Function<AuthoredMobDefinition, java.util.OptionalDouble> getter,
            double defaultValue) {
        if (authored == null) {
            return defaultValue;
        }
        java.util.OptionalDouble value = getter.apply(authored);
        return value.isPresent() ? value.getAsDouble() : defaultValue;
    }

    private static String authoredString(AuthoredMobDefinition authored,
            java.util.function.Function<AuthoredMobDefinition, Optional<String>> getter,
            String defaultValue) {
        if (authored == null) {
            return defaultValue;
        }
        return getter.apply(authored).filter(value -> !value.isBlank()).orElse(defaultValue);
    }

    private enum TierStat {
        HP,
        ATK,
        DEF,
        ACCURACY,
        CRIT,
        ASPD
    }

    private record TierModifiers(double hp, double atk, double def, double accuracy, double crit, double aspd) {
        static TierModifiers forTier(MobTier tier) {
            return switch (tier == null ? MobTier.NORMAL : tier) {
                case WEAK -> new TierModifiers(0.75D, 0.85D, 0.75D, 0.9D, 0.75D, 0.97D);
                case NORMAL -> new TierModifiers(1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);
                case ELITE -> new TierModifiers(2.2D, 1.35D, 1.25D, 1.15D, 1.35D, 1.05D);
                case BOSS -> new TierModifiers(8.0D, 2.0D, 1.8D, 1.35D, 1.8D, 1.1D);
            };
        }

        int applyInt(int value, TierStat stat) {
            double multiplier = switch (stat) {
                case HP -> hp;
                case ATK -> atk;
                case DEF -> def;
                case ACCURACY -> accuracy;
                case CRIT -> crit;
                case ASPD -> aspd;
            };
            return Math.max(0, (int) Math.round(value * multiplier));
        }
    }
}
