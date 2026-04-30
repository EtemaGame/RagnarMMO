package com.etema.ragnarmmo.combat.contract;

import java.util.Optional;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.profile.HandAttackProfile;
import com.etema.ragnarmmo.combat.status.RoCombatStatusService;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.spawn.MobProfileBootstrap;
import com.etema.ragnarmmo.mobs.util.MobProfileEligibility;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
import com.etema.ragnarmmo.player.stats.compute.StatResolutionService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public final class CombatantProfileResolver {
    private CombatantProfileResolver() {
    }

    public static Optional<CombatantProfile> resolvePlayer(ServerPlayer player, HandAttackProfile handProfile) {
        RoCombatStatusService.clearExpired(player);
        var statsOpt = RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty()) {
            return Optional.empty();
        }

        var stats = statsOpt.get();
        DerivedStats derived = StatResolutionService.computeAuthoritative(player, stats);
        if (derived == null) {
            return Optional.empty();
        }

        int str = total(player, StatKeys.STR);
        int agiPenalty = RoCombatStatusService.agiPenalty(player);
        int agi = Math.max(1, total(player, StatKeys.AGI) - agiPenalty);
        int vit = total(player, StatKeys.VIT);
        int intel = total(player, StatKeys.INT);
        int dex = total(player, StatKeys.DEX);
        int luk = total(player, StatKeys.LUK);
        int level = Math.max(1, stats.getLevel());
        double critShield = Math.floor(level / 15.0D) + Math.floor(luk / 5.0D);

        PhysicalAttackProfile physical = handProfile != null
                ? new PhysicalAttackProfile(
                        handProfile.physicalAttack(),
                        handProfile.physicalAttack(),
                        Math.max(0.0D, handProfile.accuracy()),
                        handProfile.critChance(),
                        handProfile.critDamageMultiplier(),
                        applyAspdPenalty(handProfile.aspdRo(), agiPenalty),
                        handProfile.weapon())
                : new PhysicalAttackProfile(
                        Math.max(0.0D, derived.physicalAttackMin),
                        Math.max(0.0D, derived.physicalAttackMax),
                        derived.accuracy,
                        derived.criticalChance,
                        derived.criticalDamageMultiplier,
                        applyAspdPenalty((int) Math.round(derived.attackSpeed), agiPenalty),
                        ItemStack.EMPTY);

        return Optional.of(new CombatantProfile(
                player,
                CombatantKind.PLAYER,
                new CombatStats(str, agi, vit, intel, dex, luk, level),
                physical,
                new MagicAttackProfile(derived.magicAttackMin, derived.magicAttackMax),
                new DefenseProfile(
                        applyFleePenalty(derived.flee, agiPenalty),
                        derived.perfectDodge,
                        critShield,
                        vit,
                        agi,
                        intel,
                        luk,
                        level,
                        derived.hardDefense,
                        derived.hardMagicDefense),
                new CombatModifiers(
                        "demihuman",
                        CombatPropertyResolver.getDefensiveElement(player),
                        CombatPropertyResolver.getEntitySize(player)),
                false));
    }

    public static Optional<CombatantProfile> resolveMob(Mob mob, CombatStrictMode strictMode) {
        RoCombatStatusService.clearExpired(mob);
        var stateOpt = MobProfileProvider.get(mob).resolve();
        MobProfile profile = stateOpt.filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .orElse(null);
        boolean fallback = false;

        if (profile == null) {
            profile = MobProfileBootstrap.ensureInitialized(mob, MobProfileBootstrap.InitReason.COMBAT_LAZY)
                    .orElse(null);
        }

        if (profile == null) {
            if (strictMode == CombatStrictMode.DEV) {
                return Optional.empty();
            }
            profile = MobProfileState.defaultProfile();
            fallback = true;
        }

        int level = Math.max(1, profile.level());
        RoBaseStats baseStats = profile.baseStats();
        int str = baseStats.str();
        int agiPenalty = RoCombatStatusService.agiPenalty(mob);
        int agi = Math.max(1, baseStats.agi() - agiPenalty);
        int vit = baseStats.vit();
        int intel = baseStats.intel();
        int dex = baseStats.dex();
        int luk = baseStats.luk();
        double critChance = CombatMath.clamp(0.0D, 1.0D, profile.crit() / 100.0D);
        double critShield = Math.floor(level / 15.0D) + Math.floor(luk / 5.0D);
        double atkMultiplier = RoCombatStatusService.physicalAttackMultiplier(mob);
        double defMultiplier = RoCombatStatusService.physicalDefenseMultiplier(mob);
        CombatantKind kind = MobProfileEligibility.isCompanion(mob)
                ? CombatantKind.COMPANION
                : CombatantKind.MOB;

        return Optional.of(new CombatantProfile(
                mob,
                kind,
                new CombatStats(str, agi, vit, intel, dex, luk, level),
                new PhysicalAttackProfile(
                        profile.atkMin() * atkMultiplier,
                        profile.atkMax() * atkMultiplier,
                        profile.hit(),
                        critChance,
                        CombatMath.computeCritDamageMultiplier(luk, str),
                        applyAspdPenalty(profile.aspd(), agiPenalty),
                        ItemStack.EMPTY),
                new MagicAttackProfile(profile.matkMin(), profile.matkMax()),
                new DefenseProfile(
                        applyFleePenalty(profile.flee(), agiPenalty),
                        CombatMath.computePerfectDodge(luk),
                        critShield,
                        vit,
                        agi,
                        intel,
                        luk,
                        level,
                        profile.def() * defMultiplier,
                        profile.mdef()),
                new CombatModifiers(
                        profile.race(),
                        CombatPropertyResolver.getDefensiveElement(mob),
                        CombatPropertyResolver.getEntitySize(mob)),
                fallback));
    }

    private static int total(ServerPlayer player, StatKeys key) {
        return (int) Math.round(StatAttributes.getTotal(player, key));
    }

    private static double applyFleePenalty(double flee, int agiPenalty) {
        return Math.max(0.0D, flee - Math.max(0, agiPenalty));
    }

    private static int applyAspdPenalty(int aspdRo, int agiPenalty) {
        return Math.max(1, aspdRo - (int) Math.ceil(Math.max(0, agiPenalty) * 0.5D));
    }
}
