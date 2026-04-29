package com.etema.ragnarmmo.combat.contract;

import java.util.Optional;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.profile.HandAttackProfile;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
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
        int agi = total(player, StatKeys.AGI);
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
                        handProfile.accuracy(),
                        handProfile.critChance(),
                        handProfile.critDamageMultiplier(),
                        handProfile.aspdRo(),
                        handProfile.weapon())
                : new PhysicalAttackProfile(
                        Math.max(0.0D, derived.physicalAttackMin),
                        Math.max(0.0D, derived.physicalAttackMax),
                        derived.accuracy,
                        derived.criticalChance,
                        derived.criticalDamageMultiplier,
                        (int) Math.round(derived.attackSpeed),
                        ItemStack.EMPTY);

        return Optional.of(new CombatantProfile(
                player,
                new CombatStats(str, agi, vit, intel, dex, luk, level),
                physical,
                new MagicAttackProfile(derived.magicAttackMin, derived.magicAttackMax),
                new DefenseProfile(
                        derived.flee,
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
        var stateOpt = MobProfileProvider.get(mob).resolve();
        MobProfile profile = stateOpt.filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .orElse(null);
        boolean fallback = false;

        if (profile == null) {
            if (strictMode == CombatStrictMode.DEV) {
                return Optional.empty();
            }
            profile = MobProfileState.defaultProfile();
            fallback = true;
        }

        int level = Math.max(1, profile.level());
        int dexEstimate = Math.max(1, profile.hit() - level);
        int agiEstimate = Math.max(1, profile.flee() - level);
        int lukEstimate = Math.max(1, profile.crit());
        int vitEstimate = Math.max(1, profile.def());
        int intEstimate = Math.max(1, profile.mdef());
        double critChance = CombatMath.clamp(0.0D, 1.0D, profile.crit() / 100.0D);
        double critShield = Math.floor(level / 15.0D) + Math.floor(lukEstimate / 5.0D);

        return Optional.of(new CombatantProfile(
                mob,
                new CombatStats(1, agiEstimate, vitEstimate, intEstimate, dexEstimate, lukEstimate, level),
                new PhysicalAttackProfile(
                        profile.atkMin(),
                        profile.atkMax(),
                        profile.hit(),
                        critChance,
                        CombatMath.computeCritDamageMultiplier(lukEstimate, 1),
                        profile.aspd(),
                        ItemStack.EMPTY),
                new MagicAttackProfile(0.0D, 0.0D),
                new DefenseProfile(
                        profile.flee(),
                        CombatMath.computePerfectDodge(lukEstimate),
                        critShield,
                        vitEstimate,
                        agiEstimate,
                        intEstimate,
                        lukEstimate,
                        level,
                        profile.def(),
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
}
