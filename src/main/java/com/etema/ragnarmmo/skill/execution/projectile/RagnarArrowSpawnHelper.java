package com.etema.ragnarmmo.skill.execution.projectile;

import java.util.function.Consumer;

import com.etema.ragnarmmo.roitems.runtime.RangedWeaponStatsHelper;
import com.etema.ragnarmmo.skill.data.SkillDefinition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Shared safe arrow spawner for server-side Ragnar ranged skills.
 */
public final class RagnarArrowSpawnHelper {

    private RagnarArrowSpawnHelper() {
    }

    public static Arrow spawn(ServerPlayer player, Vec3 direction, float velocity, float inaccuracy, float drawRatio,
            Consumer<CompoundTag> snapshotMutator) {
        return spawn(player, null, 0, direction, velocity, inaccuracy, drawRatio, null, snapshotMutator);
    }

    public static Arrow spawn(ServerPlayer player, SkillDefinition definition, int level, Vec3 direction, float velocity,
            float inaccuracy, float drawRatio, Consumer<CompoundTag> snapshotMutator) {
        return spawn(player, definition, level, direction, velocity, inaccuracy, drawRatio, null, snapshotMutator);
    }

    public static Arrow spawn(ServerPlayer player, Vec3 direction, float velocity, float inaccuracy, float drawRatio,
            Consumer<Arrow> arrowMutator, Consumer<CompoundTag> snapshotMutator) {
        return spawn(player, null, 0, direction, velocity, inaccuracy, drawRatio, arrowMutator, snapshotMutator);
    }

    public static Arrow spawn(ServerPlayer player, SkillDefinition definition, int level, Vec3 direction, float velocity,
            float inaccuracy, float drawRatio, Consumer<Arrow> arrowMutator,
            Consumer<CompoundTag> snapshotMutator) {
        ProjectileSkillHelper.ProjectileTuning tuning = ProjectileSkillHelper.resolveTuning(player, definition, level);
        Arrow arrow = new Arrow(player.level(), player);
        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(0.5D));
        Vec3 shotDirection = sanitizeDirection(player, direction);

        arrow.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        arrow.shoot(shotDirection.x, shotDirection.y, shotDirection.z,
                velocity * (float) tuning.velocityMult(), inaccuracy * (float) tuning.spreadMult());
        arrow.setCritArrow(false);
        ProjectileSkillHelper.applyGravityMultiplier(arrow, tuning.gravityMult());
        arrow.getPersistentData().putBoolean(ProjectileSkillHelper.PROJECTILE_TUNING_APPLIED_TAG, true);

        if (arrowMutator != null) {
            arrowMutator.accept(arrow);
        }

        ItemStack weapon = resolveRangedWeapon(player);
        if (!weapon.isEmpty()) {
            RangedWeaponStatsHelper.snapshotArrow(arrow, player, weapon, drawRatio);
            mutateSnapshot(arrow, snapshotMutator);
        }

        player.level().addFreshEntity(arrow);
        return arrow;
    }

    private static Vec3 sanitizeDirection(ServerPlayer player, Vec3 direction) {
        Vec3 fallback = player.getLookAngle();
        if (direction == null || direction.lengthSqr() <= 1.0E-7D) {
            return fallback;
        }
        return direction.normalize();
    }

    private static void mutateSnapshot(Arrow arrow, Consumer<CompoundTag> snapshotMutator) {
        if (snapshotMutator == null || !arrow.getPersistentData().contains(RangedWeaponStatsHelper.SNAPSHOT_TAG)) {
            return;
        }

        CompoundTag snapshot = arrow.getPersistentData().getCompound(RangedWeaponStatsHelper.SNAPSHOT_TAG);
        snapshotMutator.accept(snapshot);
        arrow.getPersistentData().put(RangedWeaponStatsHelper.SNAPSHOT_TAG, snapshot);
    }

    private static ItemStack resolveRangedWeapon(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (RangedWeaponStatsHelper.supports(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (RangedWeaponStatsHelper.supports(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}
