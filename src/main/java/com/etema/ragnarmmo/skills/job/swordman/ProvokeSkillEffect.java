package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.combat.aggro.AggroManager;
import com.etema.ragnarmmo.combat.status.RoCombatStatusService;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import com.etema.ragnarmmo.mobs.util.MobUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
public class ProvokeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "provoke");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(LivingEntity user, int level) {
        if (level <= 0)
            return;


        var defOpt = SkillRegistry.get(ID);
        double range = defOpt
                .map(def -> def.getLevelDouble("range", level, 5.0D))
                .orElse(5.0D);
        LivingEntity target = getTarget(user, range);
        if (target == null || target == user)
            return;

        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD || MobUtils.isBossLike(target)) {
            if (user instanceof Player player) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_blocked_type")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            return;
        }

        // Provoke only works on Mob entities (not other players)
        if (!(target instanceof Mob mob)) {
            if (user instanceof Player player) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cProvoke failed — target cannot be taunted."));
            }
            return;
        }

        user.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        int durationTicks = defOpt
                .map(def -> def.getLevelInt("duration_ticks", level, 30 * 20))
                .orElse(30 * 20);
        double baseChance = defOpt
                .map(def -> def.getLevelDouble("success_chance", level, 0.53D + ((level - 1) * 0.03D)))
                .orElse(0.53D + ((level - 1) * 0.03D));
        int levelDiff = SwordmanCombatUtil.estimateLevel(user) - SwordmanCombatUtil.estimateLevel(target);
        double successChance = Mth.clamp(baseChance + (levelDiff * 0.01), 0.05, 0.95);

        if (user.getRandom().nextDouble() > successChance) {
            if (user instanceof Player player) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Provoke §cfailed§7."));
            }
            if (user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1.3, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.01);
            }
            return;
        }

        // --- 1. AGGRO: Force the mob to retarget the casting player ---
        AggroManager.applyAggro(mob, user, durationTicks);
        mob.setTarget(user); // Immediate retarget

        double defReductionPercent = defOpt
                .map(def -> def.getLevelDouble("def_reduction_percent", level, 5.0D + (5.0D * level)))
                .orElse(5.0D + (5.0D * level));
        double atkBonusPercent = defOpt
                .map(def -> def.getLevelDouble("attack_bonus_percent", level, 2.0D + (3.0D * level)))
                .orElse(2.0D + (3.0D * level));
        RoCombatStatusService.applyProvoke(mob, durationTicks, defReductionPercent, atkBonusPercent);

        // --- 3. FEEDBACK ---
        user.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                RagnarSounds.PROVOKE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    target.getX(), target.getY() + 1.8, target.getZ(),
                    15, 0.3, 0.2, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + 1.2, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.0);
            SkillVisualFx.spawnRotatingRing(serverLevel, target.position(), 0.8, target.getBbHeight() + 0.15,
                    ParticleTypes.ANGRY_VILLAGER, 7, 0.0);
            SkillVisualFx.spawnFrontArc(serverLevel, user, 2.0, 1.8, 1.2,
                    ParticleTypes.DAMAGE_INDICATOR, ParticleTypes.SMOKE, 7);
        }
    }

    private LivingEntity getTarget(LivingEntity user, double range) {
        Vec3 start = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        AABB searchBox = user.getBoundingBox().inflate(range);

        List<LivingEntity> possibleTargets = user.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
