package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.player.party.Party;
import com.etema.ragnarmmo.player.party.PartyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class AcolyteTargetingHelper {

    private AcolyteTargetingHelper() {
    }

    @Nullable
    static LivingEntity raycast(ServerPlayer player, double range, Predicate<LivingEntity> filter) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox().inflate(range);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != player && entity.isAlive() && filter.test(entity));

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : candidates) {
            AABB targetBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.35);
            var hit = targetBox.clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = entity;
                }
            }
        }

        return closestTarget;
    }

    static LivingEntity resolveSupportTarget(ServerPlayer player, double range) {
        LivingEntity target = raycast(player, range, entity -> isSupportTarget(player, entity));
        return target != null ? target : player;
    }

    static LivingEntity resolveHealTarget(ServerPlayer player, double range) {
        if (player.isShiftKeyDown()) {
            LivingEntity hostile = raycast(player, range, entity -> entity.getMobType() == MobType.UNDEAD);
            if (hostile != null) {
                return hostile;
            }
        }

        LivingEntity target = raycast(player, range, entity -> isSupportTarget(player, entity));
        return target != null ? target : player;
    }

    @Nullable
    static LivingEntity resolveHostileTarget(ServerPlayer player, double range) {
        return raycast(player, range, entity -> isHostileTarget(player, entity));
    }

    static List<ServerPlayer> collectPartyMembersInRange(ServerPlayer player, double range) {
        Set<ServerPlayer> targets = new LinkedHashSet<>();
        targets.add(player);

        if (player.getServer() == null) {
            return new ArrayList<>(targets);
        }

        Party party = PartyService.get(player.getServer()).getParty(player);
        if (party == null) {
            return new ArrayList<>(targets);
        }

        double rangeSq = range * range;
        for (ServerPlayer member : party.getOnlineMembers(player.getServer())) {
            if (!member.isAlive()) {
                continue;
            }
            if (!member.level().dimension().equals(player.level().dimension())) {
                continue;
            }
            if (player.distanceToSqr(member) <= rangeSq) {
                targets.add(member);
            }
        }

        return new ArrayList<>(targets);
    }

    static boolean isSupportTarget(ServerPlayer player, LivingEntity entity) {
        if (!entity.isAlive() || entity.getMobType() == MobType.UNDEAD) {
            return false;
        }
        if (entity instanceof ServerPlayer) {
            return true;
        }
        if (entity instanceof Enemy) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
            return true;
        }
        return !(entity instanceof Mob);
    }

    static boolean isHostileTarget(ServerPlayer player, LivingEntity entity) {
        if (!entity.isAlive() || entity == player) {
            return false;
        }
        if (entity.getMobType() == MobType.UNDEAD || entity instanceof Enemy) {
            return true;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
            return false;
        }
        if (entity instanceof NeutralMob neutralMob) {
            return neutralMob.getTarget() == player || neutralMob.isAngryAt(player);
        }
        return entity instanceof Mob mob && mob.getTarget() == player;
    }

    static boolean isSignumTarget(LivingEntity entity) {
        return entity.getMobType() == MobType.UNDEAD || entity.getMobType() == MobType.ILLAGER;
    }
}
