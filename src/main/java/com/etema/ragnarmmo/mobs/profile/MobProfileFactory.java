package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DefaultProfile;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.FormulaRules;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyResult;

import java.util.Optional;

public final class MobProfileFactory {
    public MobProfile create(DifficultyResult difficulty, Optional<AuthoredMobDefinition> authoredDefinition) {
        DefaultProfile defaults = MobConfigAccess.getDefaultProfile();
        FormulaRules formulas = MobConfigAccess.getFormulaRules();
        AuthoredMobDefinition authored = authoredDefinition.orElse(null);
        int level = difficulty.level();

        int maxHp = authoredInt(authored, AuthoredMobDefinition::baseHp,
                defaults.maxHp() + scaled(level, formulas.hpPerLevel()));
        int atkMin = authoredInt(authored, AuthoredMobDefinition::atkMin,
                defaults.atkMin() + scaled(level, formulas.atkMinPerLevel()));
        int atkMax = Math.max(atkMin, authoredInt(authored, AuthoredMobDefinition::atkMax,
                Math.max(atkMin, defaults.atkMax() + scaled(level, formulas.atkMinPerLevel() + formulas.atkMaxExtraPerLevel()))));
        int def = authoredInt(authored, AuthoredMobDefinition::def, defaults.def() + scaled(level, formulas.defPerLevel()));
        int mdef = authoredInt(authored, AuthoredMobDefinition::mdef, defaults.mdef() + scaled(level, formulas.mdefPerLevel()));
        int hit = authoredInt(authored, AuthoredMobDefinition::hit, defaults.hit() + scaled(level, formulas.hitPerLevel()));
        int flee = authoredInt(authored, AuthoredMobDefinition::flee, defaults.flee() + scaled(level, formulas.fleePerLevel()));
        int crit = authoredInt(authored, AuthoredMobDefinition::crit, defaults.crit());
        int aspd = authoredInt(authored, AuthoredMobDefinition::aspd, defaults.aspd() + scaled(level, formulas.aspdPerLevel()));
        double moveSpeed = authoredDouble(authored, AuthoredMobDefinition::moveSpeed,
                Math.min(formulas.moveSpeedCap(), defaults.moveSpeed() + (level * formulas.moveSpeedPerLevel())));

        String race = authoredString(authored, AuthoredMobDefinition::race, defaults.race());
        String element = authoredString(authored, AuthoredMobDefinition::element, defaults.element());
        String size = authoredString(authored, AuthoredMobDefinition::size, defaults.size());

        return new MobProfile(level, difficulty.rank(), maxHp, atkMin, atkMax, def, mdef, hit, flee, crit, aspd,
                moveSpeed, race, element, size);
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
}
