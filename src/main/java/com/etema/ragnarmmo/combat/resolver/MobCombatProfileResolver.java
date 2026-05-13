package com.etema.ragnarmmo.combat.resolver;

import com.etema.ragnarmmo.combat.formula.AspdFormulaService;
import com.etema.ragnarmmo.combat.formula.FormulaUtil;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class MobCombatProfileResolver {
    private MobCombatProfileResolver() {
    }

    public static OptionalInt tryGetResolvedMobHit(LivingEntity entity) {
        if (entity instanceof Player) return OptionalInt.empty();
        return MobProfileProvider.get(entity).resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .filter(profile -> profile.hit() > 0)
                .map(profile -> OptionalInt.of(profile.hit()))
                .orElse(OptionalInt.empty());
    }

    public static OptionalInt tryGetResolvedMobFlee(LivingEntity entity) {
        if (entity instanceof Player) return OptionalInt.empty();
        return MobProfileProvider.get(entity).resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .filter(profile -> profile.flee() > 0)
                .map(profile -> OptionalInt.of(profile.flee()))
                .orElse(OptionalInt.empty());
    }

    public static OptionalDouble tryGetResolvedMobCritChance(LivingEntity entity) {
        if (entity instanceof Player) return OptionalDouble.empty();
        return MobProfileProvider.get(entity).resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .map(profile -> OptionalDouble.of(FormulaUtil.clamp(0.0D, 1.0D, profile.crit() / 100.0D)))
                .orElse(OptionalDouble.empty());
    }

    public static OptionalInt tryGetResolvedMobAspd(LivingEntity entity) {
        if (entity instanceof Player) return OptionalInt.empty();
        return MobProfileProvider.get(entity).resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .filter(profile -> profile.aspd() > 0)
                .map(profile -> OptionalInt.of(profile.aspd()))
                .orElse(OptionalInt.empty());
    }

    public static OptionalInt tryGetResolvedMobAttackIntervalTicks(LivingEntity entity) {
        OptionalInt aspd = tryGetResolvedMobAspd(entity);
        if (aspd.isEmpty()) return OptionalInt.empty();
        double aps = AspdFormulaService.attacksPerSecond(aspd.getAsInt());
        return OptionalInt.of(Math.max(2, (int) Math.round(20.0D / aps)));
    }
}
