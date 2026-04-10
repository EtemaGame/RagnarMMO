package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobDirectStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobRoStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobTemplate;
import com.etema.ragnarmmo.common.api.mobs.data.ResolvedMobDefinition;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobBaseStats;
import com.etema.ragnarmmo.common.api.mobs.data.load.MobDefinitionRegistry;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionIssue;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionResult;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure orchestrator for the new strict manual mob path.
 *
 * <p>This resolver performs exact-match definition lookup, declarative resolution, and construction of
 * {@link ComputedMobProfile}. It does not fall back to automatic modes and does not apply anything to
 * runtime entities.</p>
 */
public final class ManualMobProfileResolver {

    private ManualMobProfileResolver() {
    }

    public static ManualMobProfileResolutionResult resolve(ResourceLocation entityTypeId) {
        return resolve(entityTypeId, MobDefinitionRegistry.getInstance());
    }

    public static ManualMobProfileResolutionResult resolve(
            ResourceLocation entityTypeId,
            MobDefinitionRegistry registry) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(registry, "registry");

        MobDefinition definition = registry.getDefinition(entityTypeId).orElse(null);
        if (definition == null) {
            return failure(missingCoverage(
                    "entity_type",
                    "no manual mob definition is registered for entity_type id " + entityTypeId));
        }

        MobTemplate template = null;
        if (definition.template() != null) {
            template = registry.getTemplate(definition.template()).orElse(null);
            if (template == null) {
                return failure(invalid(
                        "template",
                        "referenced template is missing from registry: " + definition.template()));
            }
        }

        MobDefinitionResolutionResult declarativeResult = MobDefinitionResolver.resolve(definition, template);
        if (!declarativeResult.issues().isEmpty()) {
            return new ManualMobProfileResolutionResult(null, mapDeclarativeIssues(declarativeResult.issues()));
        }

        ResolvedMobDefinition resolvedDefinition = declarativeResult.definition();
        List<ManualMobProfileIssue> issues = new ArrayList<>();

        Integer level = resolveManualLevel(resolvedDefinition, issues);
        MobRank rank = resolveManualRank(resolvedDefinition, issues);
        String race = requireResolvedText("race", resolvedDefinition.race(), issues);
        String element = requireResolvedText("element", resolvedDefinition.element(), issues);
        String size = requireResolvedText("size", resolvedDefinition.size(), issues);

        MobDirectStatsBlock directStats = resolvedDefinition.directStats();
        MobRoStatsBlock roStats = resolvedDefinition.roStats();

        Integer maxHp = resolveFinalIntStat("max_hp", directStats != null ? directStats.maxHp() : null, level, roStats, issues);
        Integer atkMin = resolveFinalIntStat("atk_min", directStats != null ? directStats.atkMin() : null, level, roStats, issues);
        Integer atkMax = resolveFinalIntStat("atk_max", directStats != null ? directStats.atkMax() : null, level, roStats, issues);
        Integer def = resolveFinalIntStat("def", directStats != null ? directStats.def() : null, level, roStats, issues);
        Integer mdef = resolveFinalIntStat("mdef", directStats != null ? directStats.mdef() : null, level, roStats, issues);
        Integer hit = resolveFinalIntStat("hit", directStats != null ? directStats.hit() : null, level, roStats, issues);
        Integer flee = resolveFinalIntStat("flee", directStats != null ? directStats.flee() : null, level, roStats, issues);
        Integer crit = resolveFinalIntStat("crit", directStats != null ? directStats.crit() : null, level, roStats, issues);
        Integer aspd = resolveFinalIntStat("aspd", directStats != null ? directStats.aspd() : null, level, roStats, issues);
        Double moveSpeed = resolveFinalDoubleStat(
                "move_speed",
                directStats != null ? directStats.moveSpeed() : null,
                roStats,
                issues);
        ComputedMobBaseStats baseCombatStats = resolveOptionalBaseCombatStats(roStats);

        if (!issues.isEmpty()) {
            return new ManualMobProfileResolutionResult(null, issues);
        }

        try {
            return new ManualMobProfileResolutionResult(
                    new ComputedMobProfile(
                            level,
                            rank,
                            maxHp,
                            atkMin,
                            atkMax,
                            def,
                            mdef,
                            hit,
                            flee,
                            crit,
                            aspd,
                            moveSpeed,
                            baseCombatStats,
                            race,
                            element,
                            size),
                    List.of());
        } catch (IllegalArgumentException ex) {
            return failure(invalid("computed_profile", ex.getMessage()));
        }
    }

    private static @Nullable Integer resolveManualLevel(
            ResolvedMobDefinition definition,
            List<ManualMobProfileIssue> issues) {
        if (definition.level() == null) {
            issues.add(incomplete("level", "level is unresolved for the manual runtime profile"));
        }
        return definition.level();
    }

    private static @Nullable MobRank resolveManualRank(
            ResolvedMobDefinition definition,
            List<ManualMobProfileIssue> issues) {
        if (definition.rank() == null) {
            issues.add(incomplete("rank", "rank is unresolved for the manual runtime profile"));
        }
        return definition.rank();
    }

    private static @Nullable String requireResolvedText(
            String field,
            @Nullable String value,
            List<ManualMobProfileIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(incomplete(field, field + " is unresolved for the manual runtime profile"));
            return null;
        }
        return value;
    }

    private static @Nullable Integer resolveFinalIntStat(
            String field,
            @Nullable Integer directValue,
            @Nullable Integer level,
            @Nullable MobRoStatsBlock roStats,
            List<ManualMobProfileIssue> issues) {
        if (directValue != null) {
            return directValue;
        }
        if (hasCompleteRoStats(roStats)) {
            Integer derivedValue = tryDeriveFinalIntStat(field, level, roStats);
            if (derivedValue != null) {
                return derivedValue;
            }
            if (requiresResolvedLevelForDerivation(field) && level == null) {
                issues.add(incomplete(
                        field,
                        field + " requires a resolved level before it can derive from complete ro_stats"));
                return null;
            }
            issues.add(derivationUnimplemented(
                    field,
                    field + " requires derivation from complete ro_stats, but manual stat derivation is not implemented yet"));
        } else {
            issues.add(incomplete(field, field + " is unresolved for the manual runtime profile"));
        }
        return null;
    }

    private static boolean requiresResolvedLevelForDerivation(String field) {
        return "hit".equals(field) || "flee".equals(field);
    }

    private static @Nullable Integer tryDeriveFinalIntStat(
            String field,
            @Nullable Integer level,
            @Nullable MobRoStatsBlock roStats) {
        if (!hasCompleteRoStats(roStats)) {
            return null;
        }

        // Only derive fields whose current numeric formula is already represented in the
        // runtime combat helpers. Broader scalar derivation remains intentionally deferred.
        return switch (field) {
            case "hit" -> level == null
                    ? null
                    : (int) Math.round(CombatMath.computeHIT(roStats.dex(), roStats.luk(), level, 0.0D));
            case "flee" -> level == null
                    ? null
                    : (int) Math.round(CombatMath.computeFLEE(roStats.agi(), roStats.luk(), level, 0.0D));
            case "crit" -> (int) Math.round(CombatMath.computeCritChance(roStats.luk(), roStats.dex(), 0.0D) * 100.0D);
            default -> null;
        };
    }

    private static @Nullable Double resolveFinalDoubleStat(
            String field,
            @Nullable Double directValue,
            @Nullable MobRoStatsBlock roStats,
            List<ManualMobProfileIssue> issues) {
        if (directValue != null) {
            return directValue;
        }
        if (hasCompleteRoStats(roStats)) {
            issues.add(derivationUnimplemented(
                    field,
                    field + " requires derivation from complete ro_stats, but manual stat derivation is not implemented yet"));
        } else {
            issues.add(incomplete(field, field + " is unresolved for the manual runtime profile"));
        }
        return null;
    }

    private static boolean hasCompleteRoStats(@Nullable MobRoStatsBlock roStats) {
        return roStats != null
                && roStats.str() != null
                && roStats.agi() != null
                && roStats.vit() != null
                && roStats.int_() != null
                && roStats.dex() != null
                && roStats.luk() != null;
    }

    private static @Nullable ComputedMobBaseStats resolveOptionalBaseCombatStats(@Nullable MobRoStatsBlock roStats) {
        if (roStats == null
                || roStats.vit() == null
                || roStats.int_() == null
                || roStats.agi() == null
                || roStats.luk() == null) {
            return null;
        }

        return new ComputedMobBaseStats(
                roStats.str(),
                roStats.vit(),
                roStats.int_(),
                roStats.agi(),
                roStats.luk(),
                roStats.dex());
    }

    private static List<ManualMobProfileIssue> mapDeclarativeIssues(List<MobDefinitionResolutionIssue> issues) {
        List<ManualMobProfileIssue> mapped = new ArrayList<>(issues.size());
        for (MobDefinitionResolutionIssue issue : issues) {
            mapped.add(new ManualMobProfileIssue(
                    mapDeclarativeKind(issue.kind()),
                    issue.field(),
                    issue.message()));
        }
        return mapped;
    }

    private static ManualMobProfileIssue.Kind mapDeclarativeKind(MobDefinitionResolutionIssue.Kind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case INVALID -> ManualMobProfileIssue.Kind.INVALID;
            case INCOMPLETE -> ManualMobProfileIssue.Kind.INCOMPLETE;
        };
    }

    private static ManualMobProfileResolutionResult failure(ManualMobProfileIssue issue) {
        return new ManualMobProfileResolutionResult(null, List.of(issue));
    }

    private static ManualMobProfileIssue missingCoverage(String field, String message) {
        return new ManualMobProfileIssue(ManualMobProfileIssue.Kind.MISSING_COVERAGE, field, message);
    }

    private static ManualMobProfileIssue invalid(String field, String message) {
        return new ManualMobProfileIssue(ManualMobProfileIssue.Kind.INVALID, field, message);
    }

    private static ManualMobProfileIssue incomplete(String field, String message) {
        return new ManualMobProfileIssue(ManualMobProfileIssue.Kind.INCOMPLETE, field, message);
    }

    private static ManualMobProfileIssue derivationUnimplemented(String field, String message) {
        return new ManualMobProfileIssue(ManualMobProfileIssue.Kind.DERIVATION_UNIMPLEMENTED, field, message);
    }
}
