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
        MobTier tier = authored == null
                ? MobTier.fromRank(difficulty.rank())
                : authored.tier().orElse(MobTier.fromRank(difficulty.rank()));
        TierModifiers modifiers = TierModifiers.forTier(tier);

        int maxHp = authoredInt(authored, AuthoredMobDefinition::baseHp,
                modifiers.applyInt(defaults.maxHp() + scaled(level, formulas.hpPerLevel()), TierStat.HP));
        int atkMin = authoredInt(authored, AuthoredMobDefinition::atkMin,
                modifiers.applyInt(defaults.atkMin() + scaled(level, formulas.atkMinPerLevel()), TierStat.ATK));
        int atkMax = Math.max(atkMin, authoredInt(authored, AuthoredMobDefinition::atkMax,
                Math.max(atkMin, modifiers.applyInt(defaults.atkMax() + scaled(level, formulas.atkMinPerLevel() + formulas.atkMaxExtraPerLevel()), TierStat.ATK))));
        int def = authoredInt(authored, AuthoredMobDefinition::def, modifiers.applyInt(defaults.def() + scaled(level, formulas.defPerLevel()), TierStat.DEF));
        int mdef = authoredInt(authored, AuthoredMobDefinition::mdef, modifiers.applyInt(defaults.mdef() + scaled(level, formulas.mdefPerLevel()), TierStat.DEF));
        int hit = authoredInt(authored, AuthoredMobDefinition::hit, modifiers.applyInt(defaults.hit() + scaled(level, formulas.hitPerLevel()), TierStat.ACCURACY));
        int flee = authoredInt(authored, AuthoredMobDefinition::flee, modifiers.applyInt(defaults.flee() + scaled(level, formulas.fleePerLevel()), TierStat.ACCURACY));
        int crit = authoredInt(authored, AuthoredMobDefinition::crit, modifiers.applyInt(defaults.crit(), TierStat.CRIT));
        int aspd = authoredInt(authored, AuthoredMobDefinition::aspd, modifiers.applyInt(defaults.aspd() + scaled(level, formulas.aspdPerLevel()), TierStat.ASPD));
        double moveSpeed = authoredDouble(authored, AuthoredMobDefinition::moveSpeed,
                Math.min(formulas.moveSpeedCap(), defaults.moveSpeed() + (level * formulas.moveSpeedPerLevel())));
        int baseExp = authoredInt(authored, AuthoredMobDefinition::baseExp, MobRewardFormula.baseExp(level, tier));
        int jobExp = authoredInt(authored, AuthoredMobDefinition::jobExp, MobRewardFormula.jobExp(level, tier));

        String race = authoredString(authored, AuthoredMobDefinition::race, defaults.race());
        String element = authoredString(authored, AuthoredMobDefinition::element, defaults.element());
        String size = authoredString(authored, AuthoredMobDefinition::size, defaults.size());

        return new MobProfile(level, difficulty.rank(), tier, maxHp, atkMin, atkMax, def, mdef, hit, flee, crit, aspd,
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
