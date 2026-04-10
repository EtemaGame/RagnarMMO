package com.etema.ragnarmmo.common.api.mobs.data.resolve;

import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobDirectStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobRoStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobTemplate;
import com.etema.ragnarmmo.common.api.mobs.data.ResolvedMobDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure declarative resolver for manual mob definitions.
 *
 * <p>This class only handles authored merge, override precedence, and declarative validation. It does not
 * perform datapack loading, taxonomy fallback, or runtime stat derivation.</p>
 */
public final class MobDefinitionResolver {

    private static final String[] REQUIRED_FINAL_COMBAT_FIELDS = {
            "maxHp",
            "atkMin",
            "atkMax",
            "def",
            "mdef",
            "hit",
            "flee",
            "crit",
            "aspd",
            "moveSpeed"
    };

    private MobDefinitionResolver() {
    }

    public static MobDefinitionResolutionResult resolve(MobDefinition definition, @Nullable MobTemplate template) {
        Objects.requireNonNull(definition, "definition");

        ResolvedMobDefinition resolved = new ResolvedMobDefinition(
                definition.entity(),
                firstNonNull(definition.rank(), template != null ? template.rank() : null),
                firstNonNull(definition.level(), template != null ? template.level() : null),
                mergeRoStats(template != null ? template.roStats() : null, definition.roStats()),
                mergeDirectStats(template != null ? template.directStats() : null, definition.directStats()),
                firstNonNull(definition.race(), template != null ? template.race() : null),
                firstNonNull(definition.element(), template != null ? template.element() : null),
                firstNonNull(definition.size(), template != null ? template.size() : null));

        return new MobDefinitionResolutionResult(resolved, validate(resolved));
    }

    public static List<MobDefinitionResolutionIssue> validate(ResolvedMobDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        List<MobDefinitionResolutionIssue> issues = new ArrayList<>();
        validateRequiredTopLevel(definition, issues);
        validateRoStats(definition.roStats(), issues);
        validateDirectStats(definition.directStats(), issues);
        validateCompleteness(definition, issues);
        return issues;
    }

    private static void validateRequiredTopLevel(
            ResolvedMobDefinition definition,
            List<MobDefinitionResolutionIssue> issues) {
        if (definition.entity() == null) {
            issues.add(invalid("entity", "entity is required"));
        }
        if (definition.level() == null) {
            issues.add(incomplete("level", "level is unresolved after declarative merge"));
        } else if (definition.level() < 1) {
            issues.add(invalid("level", "level must be >= 1"));
        }
        if (definition.rank() == null) {
            issues.add(incomplete("rank", "rank is unresolved after declarative merge"));
        }
    }

    private static void validateRoStats(
            @Nullable MobRoStatsBlock roStats,
            List<MobDefinitionResolutionIssue> issues) {
        if (roStats == null) {
            return;
        }
        validateNonNegative(roStats.str(), "roStats.str", issues);
        validateNonNegative(roStats.agi(), "roStats.agi", issues);
        validateNonNegative(roStats.vit(), "roStats.vit", issues);
        validateNonNegative(roStats.int_(), "roStats.int_", issues);
        validateNonNegative(roStats.dex(), "roStats.dex", issues);
        validateNonNegative(roStats.luk(), "roStats.luk", issues);
    }

    private static void validateDirectStats(
            @Nullable MobDirectStatsBlock directStats,
            List<MobDefinitionResolutionIssue> issues) {
        if (directStats == null) {
            return;
        }
        validatePositive(directStats.maxHp(), "directStats.maxHp", issues);
        validateNonNegative(directStats.atkMin(), "directStats.atkMin", issues);
        validateNonNegative(directStats.atkMax(), "directStats.atkMax", issues);
        validateNonNegative(directStats.def(), "directStats.def", issues);
        validateNonNegative(directStats.mdef(), "directStats.mdef", issues);
        validateNonNegative(directStats.hit(), "directStats.hit", issues);
        validateNonNegative(directStats.flee(), "directStats.flee", issues);
        validateNonNegative(directStats.crit(), "directStats.crit", issues);
        validatePositive(directStats.aspd(), "directStats.aspd", issues);
        validatePositive(directStats.moveSpeed(), "directStats.moveSpeed", issues);

        if (directStats.atkMin() != null && directStats.atkMax() != null && directStats.atkMax() < directStats.atkMin()) {
            issues.add(invalid("directStats.atkMax", "atkMax must be >= atkMin"));
        }
    }

    private static void validateCompleteness(
            ResolvedMobDefinition definition,
            List<MobDefinitionResolutionIssue> issues) {
        if (definition.race() == null) {
            issues.add(incomplete("race", "race is unresolved after declarative merge"));
        }
        if (definition.element() == null) {
            issues.add(incomplete("element", "element is unresolved after declarative merge"));
        }
        if (definition.size() == null) {
            issues.add(incomplete("size", "size is unresolved after declarative merge"));
        }

        boolean canDeriveMissingCombatFields = isCompleteRoStats(definition.roStats());
        MobDirectStatsBlock directStats = definition.directStats();

        for (String field : REQUIRED_FINAL_COMBAT_FIELDS) {
            if (!hasDirectStatValue(directStats, field) && !canDeriveMissingCombatFields) {
                issues.add(incomplete(field, field + " is unresolved after declarative merge"));
            }
        }

        if (!canDeriveMissingCombatFields && hasAnyRoStats(definition.roStats()) && hasMissingDirectCombatField(directStats)) {
            issues.add(incomplete("roStats", "roStats is incomplete for future derivation of missing final combat fields"));
        }
    }

    private static @Nullable MobRoStatsBlock mergeRoStats(
            @Nullable MobRoStatsBlock templateBlock,
            @Nullable MobRoStatsBlock definitionBlock) {
        if (templateBlock == null) {
            return definitionBlock;
        }
        if (definitionBlock == null) {
            return templateBlock;
        }
        return new MobRoStatsBlock(
                firstNonNull(definitionBlock.str(), templateBlock.str()),
                firstNonNull(definitionBlock.agi(), templateBlock.agi()),
                firstNonNull(definitionBlock.vit(), templateBlock.vit()),
                firstNonNull(definitionBlock.int_(), templateBlock.int_()),
                firstNonNull(definitionBlock.dex(), templateBlock.dex()),
                firstNonNull(definitionBlock.luk(), templateBlock.luk()));
    }

    private static @Nullable MobDirectStatsBlock mergeDirectStats(
            @Nullable MobDirectStatsBlock templateBlock,
            @Nullable MobDirectStatsBlock definitionBlock) {
        if (templateBlock == null) {
            return definitionBlock;
        }
        if (definitionBlock == null) {
            return templateBlock;
        }
        return new MobDirectStatsBlock(
                firstNonNull(definitionBlock.maxHp(), templateBlock.maxHp()),
                firstNonNull(definitionBlock.atkMin(), templateBlock.atkMin()),
                firstNonNull(definitionBlock.atkMax(), templateBlock.atkMax()),
                firstNonNull(definitionBlock.def(), templateBlock.def()),
                firstNonNull(definitionBlock.mdef(), templateBlock.mdef()),
                firstNonNull(definitionBlock.hit(), templateBlock.hit()),
                firstNonNull(definitionBlock.flee(), templateBlock.flee()),
                firstNonNull(definitionBlock.crit(), templateBlock.crit()),
                firstNonNull(definitionBlock.aspd(), templateBlock.aspd()),
                firstNonNull(definitionBlock.moveSpeed(), templateBlock.moveSpeed()));
    }

    private static boolean isCompleteRoStats(@Nullable MobRoStatsBlock roStats) {
        return roStats != null
                && roStats.str() != null
                && roStats.agi() != null
                && roStats.vit() != null
                && roStats.int_() != null
                && roStats.dex() != null
                && roStats.luk() != null;
    }

    private static boolean hasAnyRoStats(@Nullable MobRoStatsBlock roStats) {
        return roStats != null
                && (roStats.str() != null
                || roStats.agi() != null
                || roStats.vit() != null
                || roStats.int_() != null
                || roStats.dex() != null
                || roStats.luk() != null);
    }

    private static boolean hasMissingDirectCombatField(@Nullable MobDirectStatsBlock directStats) {
        for (String field : REQUIRED_FINAL_COMBAT_FIELDS) {
            if (!hasDirectStatValue(directStats, field)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDirectStatValue(@Nullable MobDirectStatsBlock directStats, String field) {
        if (directStats == null) {
            return false;
        }
        return switch (field) {
            case "maxHp" -> directStats.maxHp() != null;
            case "atkMin" -> directStats.atkMin() != null;
            case "atkMax" -> directStats.atkMax() != null;
            case "def" -> directStats.def() != null;
            case "mdef" -> directStats.mdef() != null;
            case "hit" -> directStats.hit() != null;
            case "flee" -> directStats.flee() != null;
            case "crit" -> directStats.crit() != null;
            case "aspd" -> directStats.aspd() != null;
            case "moveSpeed" -> directStats.moveSpeed() != null;
            default -> false;
        };
    }

    private static void validateNonNegative(
            @Nullable Integer value,
            String field,
            List<MobDefinitionResolutionIssue> issues) {
        if (value != null && value < 0) {
            issues.add(invalid(field, field + " must be >= 0"));
        }
    }

    private static void validatePositive(
            @Nullable Integer value,
            String field,
            List<MobDefinitionResolutionIssue> issues) {
        if (value != null && value <= 0) {
            issues.add(invalid(field, field + " must be > 0"));
        }
    }

    private static void validatePositive(
            @Nullable Double value,
            String field,
            List<MobDefinitionResolutionIssue> issues) {
        if (value != null && value <= 0.0D) {
            issues.add(invalid(field, field + " must be > 0"));
        }
    }

    private static <T> @Nullable T firstNonNull(@Nullable T preferred, @Nullable T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static MobDefinitionResolutionIssue invalid(String field, String message) {
        return new MobDefinitionResolutionIssue(MobDefinitionResolutionIssue.Kind.INVALID, field, message);
    }

    private static MobDefinitionResolutionIssue incomplete(String field, String message) {
        return new MobDefinitionResolutionIssue(MobDefinitionResolutionIssue.Kind.INCOMPLETE, field, message);
    }
}
