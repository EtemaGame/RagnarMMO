package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobDirectStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobRoStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobTemplate;
import com.etema.ragnarmmo.common.api.mobs.data.ResolvedMobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.load.MobDefinitionRegistry;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionIssue;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionResult;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolver;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class AuthoredMobProfileResolver {

    private AuthoredMobProfileResolver() {
    }

    public static boolean hasAuthoring(ResourceLocation entityTypeId) {
        return MobDefinitionRegistry.getInstance().getDefinition(entityTypeId).isPresent();
    }

    public static boolean hasResolvableProfile(ResourceLocation entityTypeId) {
        AuthoredMobProfileResolutionResult result = resolve(entityTypeId);
        return result.isSuccess() && result.profile() != null;
    }

    public static AuthoredMobProfileResolutionResult resolve(ResourceLocation entityTypeId) {
        return resolve(entityTypeId, MobDefinitionRegistry.getInstance());
    }

    public static AuthoredMobProfileResolutionResult resolve(
            ResourceLocation entityTypeId,
            MobDefinitionRegistry registry) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(registry, "registry");

        MobDefinition definition = registry.getDefinition(entityTypeId).orElse(null);
        if (definition == null) {
            return failure(missingCoverage(
                    "entity_type",
                    "no authored mob definition is registered for entity_type id " + entityTypeId));
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
            return new AuthoredMobProfileResolutionResult(null, mapDeclarativeIssues(declarativeResult.issues()));
        }

        ResolvedMobDefinition resolvedDefinition = declarativeResult.definition();
        List<AuthoredMobProfileIssue> issues = new ArrayList<>();

        Integer level = resolveLevel(resolvedDefinition, issues);
        MobRank rank = resolveRank(resolvedDefinition, issues);
        MobTier tier = resolvedDefinition.tier() != null ? resolvedDefinition.tier() : MobTier.fromRank(rank);
        String race = requireResolvedText("race", resolvedDefinition.race(), issues);
        String element = requireResolvedText("element", resolvedDefinition.element(), issues);
        String size = requireResolvedText("size", resolvedDefinition.size(), issues);

        MobDirectStatsBlock directStats = resolvedDefinition.directStats();
        MobRoStatsBlock roStats = resolvedDefinition.roStats();

        Integer maxHp = resolveFinalIntStat("max_hp", directStats != null ? directStats.maxHp() : null, level, roStats, issues);
        Integer atkMin = resolveFinalIntStat("atk_min", directStats != null ? directStats.atkMin() : null, level, roStats, issues);
        Integer atkMax = resolveFinalIntStat("atk_max", directStats != null ? directStats.atkMax() : null, level, roStats, issues);
        Integer matkMin = resolveFinalIntStat("matk_min", directStats != null ? directStats.matkMin() : null, level, roStats, issues);
        Integer matkMax = resolveFinalIntStat("matk_max", directStats != null ? directStats.matkMax() : null, level, roStats, issues);
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
        Integer baseExp = resolveExp("base_exp", resolvedDefinition.baseExp(), level, tier, issues);
        Integer jobExp = resolveExp("job_exp", resolvedDefinition.jobExp(), level, tier, issues);

        if (!issues.isEmpty()) {
            return new AuthoredMobProfileResolutionResult(null, issues);
        }

        try {
            return new AuthoredMobProfileResolutionResult(
                    new MobProfile(
                            level,
                            rank,
                            tier,
                            maxHp,
                            atkMin,
                            atkMax,
                            matkMin,
                            matkMax,
                            def,
                            mdef,
                            hit,
                            flee,
                            crit,
                            aspd,
                            moveSpeed,
                            baseExp,
                            jobExp,
                            race,
                            element,
                            size),
                    List.of());
        } catch (IllegalArgumentException ex) {
            return failure(invalid("mob_profile", ex.getMessage()));
        }
    }

    public static Optional<AuthoredMobDefinition> resolvePartialDefinition(ResourceLocation entityTypeId) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        MobDefinition definition = MobDefinitionRegistry.getInstance().getDefinition(entityTypeId).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }

        MobTemplate template = null;
        if (definition.template() != null) {
            template = MobDefinitionRegistry.getInstance().getTemplate(definition.template()).orElse(null);
            if (template == null) {
                return Optional.empty();
            }
        }

        MobDefinitionResolutionResult result = MobDefinitionResolver.resolve(definition, template);
        if (!result.issues().isEmpty() || result.definition() == null) {
            return Optional.empty();
        }

        ResolvedMobDefinition resolved = result.definition();
        MobDirectStatsBlock directStats = resolved.directStats();
        return Optional.of(new AuthoredMobDefinition(
                entityTypeId,
                Optional.ofNullable(resolved.rank()),
                Optional.ofNullable(resolved.tier()),
                optionalInt(resolved.level()),
                optionalText(resolved.race()),
                optionalText(resolved.element()),
                optionalText(resolved.size()),
                optionalInt(resolved.baseExp()),
                optionalInt(resolved.jobExp()),
                optionalInt(directStats != null ? directStats.maxHp() : null),
                optionalInt(directStats != null ? directStats.atkMin() : null),
                optionalInt(directStats != null ? directStats.atkMax() : null),
                optionalInt(directStats != null ? directStats.matkMin() : null),
                optionalInt(directStats != null ? directStats.matkMax() : null),
                optionalInt(directStats != null ? directStats.def() : null),
                optionalInt(directStats != null ? directStats.mdef() : null),
                optionalInt(directStats != null ? directStats.hit() : null),
                optionalInt(directStats != null ? directStats.flee() : null),
                optionalInt(directStats != null ? directStats.crit() : null),
                optionalInt(directStats != null ? directStats.aspd() : null),
                optionalDouble(directStats != null ? directStats.moveSpeed() : null)));
    }

    private static @Nullable Integer resolveLevel(
            ResolvedMobDefinition definition,
            List<AuthoredMobProfileIssue> issues) {
        if (definition.level() == null) {
            issues.add(incomplete("level", "level is unresolved for the authored mob profile"));
        }
        return definition.level();
    }

    private static @Nullable MobRank resolveRank(
            ResolvedMobDefinition definition,
            List<AuthoredMobProfileIssue> issues) {
        if (definition.rank() == null) {
            issues.add(incomplete("rank", "rank is unresolved for the authored mob profile"));
        }
        return definition.rank();
    }

    private static @Nullable Integer resolveExp(
            String field,
            @Nullable Integer authoredValue,
            @Nullable Integer level,
            MobTier tier,
            List<AuthoredMobProfileIssue> issues) {
        if (authoredValue != null) {
            if (authoredValue < 0) {
                issues.add(invalid(field, field + " must be >= 0"));
            }
            return authoredValue;
        }
        if (level == null) {
            issues.add(incomplete(field, field + " requires a resolved level when not authored explicitly"));
            return null;
        }
        return "base_exp".equals(field)
                ? MobRewardFormula.baseExp(level, tier)
                : MobRewardFormula.jobExp(level, tier);
    }

    private static @Nullable String requireResolvedText(
            String field,
            @Nullable String value,
            List<AuthoredMobProfileIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(incomplete(field, field + " is unresolved for the authored mob profile"));
            return null;
        }
        return value;
    }

    private static @Nullable Integer resolveFinalIntStat(
            String field,
            @Nullable Integer directValue,
            @Nullable Integer level,
            @Nullable MobRoStatsBlock roStats,
            List<AuthoredMobProfileIssue> issues) {
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
                    field + " requires derivation from complete ro_stats, but that derivation is not implemented yet"));
        } else {
            issues.add(incomplete(field, field + " is unresolved for the authored mob profile"));
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

        return switch (field) {
            case "hit" -> level == null ? null : Math.max(0, level + roStats.dex());
            case "flee" -> level == null ? null : Math.max(0, level + roStats.agi());
            case "crit" -> 0;
            case "matk_min" -> Math.max(0, roStats.int_());
            case "matk_max" -> Math.max(0, roStats.int_() + roStats.dex() / 2);
            default -> null;
        };
    }

    private static @Nullable Double resolveFinalDoubleStat(
            String field,
            @Nullable Double directValue,
            @Nullable MobRoStatsBlock roStats,
            List<AuthoredMobProfileIssue> issues) {
        if (directValue != null) {
            return directValue;
        }
        if (hasCompleteRoStats(roStats)) {
            issues.add(derivationUnimplemented(
                    field,
                    field + " requires derivation from complete ro_stats, but that derivation is not implemented yet"));
        } else {
            issues.add(incomplete(field, field + " is unresolved for the authored mob profile"));
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

    private static List<AuthoredMobProfileIssue> mapDeclarativeIssues(List<MobDefinitionResolutionIssue> issues) {
        List<AuthoredMobProfileIssue> mapped = new ArrayList<>(issues.size());
        for (MobDefinitionResolutionIssue issue : issues) {
            mapped.add(new AuthoredMobProfileIssue(
                    mapDeclarativeKind(issue.kind()),
                    issue.field(),
                    issue.message()));
        }
        return mapped;
    }

    private static AuthoredMobProfileIssue.Kind mapDeclarativeKind(MobDefinitionResolutionIssue.Kind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case INVALID -> AuthoredMobProfileIssue.Kind.INVALID;
            case INCOMPLETE -> AuthoredMobProfileIssue.Kind.INCOMPLETE;
        };
    }

    private static AuthoredMobProfileResolutionResult failure(AuthoredMobProfileIssue issue) {
        return new AuthoredMobProfileResolutionResult(null, List.of(issue));
    }

    private static AuthoredMobProfileIssue missingCoverage(String field, String message) {
        return new AuthoredMobProfileIssue(AuthoredMobProfileIssue.Kind.MISSING_COVERAGE, field, message);
    }

    private static AuthoredMobProfileIssue invalid(String field, String message) {
        return new AuthoredMobProfileIssue(AuthoredMobProfileIssue.Kind.INVALID, field, message);
    }

    private static AuthoredMobProfileIssue incomplete(String field, String message) {
        return new AuthoredMobProfileIssue(AuthoredMobProfileIssue.Kind.INCOMPLETE, field, message);
    }

    private static AuthoredMobProfileIssue derivationUnimplemented(String field, String message) {
        return new AuthoredMobProfileIssue(AuthoredMobProfileIssue.Kind.DERIVATION_UNIMPLEMENTED, field, message);
    }

    private static Optional<String> optionalText(@Nullable String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static OptionalInt optionalInt(@Nullable Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private static OptionalDouble optionalDouble(@Nullable Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }
}
