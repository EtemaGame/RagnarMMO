package com.etema.ragnarmmo.skill.job.thief;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EnvenomSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "envenom");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getMeleeTarget(player);
        
        final Vec3 lookPos = player.getEyePosition().add(player.getLookAngle().scale(2.0));

        // Introduce a slight delay for the "slash" feel
        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(2, () -> {
            double px = target != null ? target.getX() : lookPos.x;
            double py = target != null ? target.getY() : lookPos.y;
            double pz = target != null ? target.getZ() : lookPos.z;

            player.level().playSound(null, px, py, pz,
                    SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Purple toxic burst with RO "splash" feel
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, py + 1.0, pz, 
                        20, 0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, px, py + 1.0, pz, 
                        15, 0.3, 0.5, 0.3, 0.1);
            }

            if (target != null && target.isAlive()) {
                // RO: (30 + 20×level)% ATK + Poison chance (10 + 4×level)%
                float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                        com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, 30f + 20f * level));
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                        target, player.damageSources().playerAttack(player), damage);

                float poisonChance = 0.10f + (level * 0.04f); // 14% at Lv1 to 50% at Lv10
                if (player.getRandom().nextFloat() < poisonChance) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 200 + (level * 20), 0, false, true, true));
                }
            }
        });
    }

    // (Helper duplicated for clarity across active melee skills)
    private LivingEntity getMeleeTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(3.5));

        AABB searchBox = player.getBoundingBox().inflate(3.5);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

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
