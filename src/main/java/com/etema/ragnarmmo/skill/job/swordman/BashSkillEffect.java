package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.skill.execution.instant.InstantTargetSkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.data.progression.SkillProgressManager;
import com.etema.ragnarmmo.skill.data.progression.SkillProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Bash — Active melee skill (Swordman).
 * RO Formula: Single hit dealing (130 + 30 × level)% ATK.
 * Lv6+ with Fatal Blow skill learned has a (5 × (Lv - 5))% stun chance.
 */
public class BashSkillEffect extends InstantTargetSkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "bash");
    private static final ResourceLocation FATAL_BLOW = new ResourceLocation("ragnarmmo", "fatal_blow");

    public BashSkillEffect() {
        super(ID);
    }

    public BashSkillEffect(ResourceLocation id) {
        super(id);
    }

    @Override
    protected double getRange(int level) {
        return 3.5;
    }

    @Override
    protected int getAnimationDelay(int level) {
        return 3;
    }

    @Override
    protected void playInitialVisuals(LivingEntity user, @Nullable LivingEntity target, int level) {
        if (user.level().isClientSide) return;

        // Sweep flash at the moment of activation (swing)
        if (user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    user.getX() + user.getLookAngle().x,
                    user.getY() + 1.2,
                    user.getZ() + user.getLookAngle().z,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void applyEffect(LivingEntity user, @Nullable LivingEntity target, int level) {
        if (target == null || !target.isAlive()) return;

        // RO: (130 + 30 × level)% ATK
        float pct = 130.0f + (30.0f * level);
        float damage = Math.max(SkillDamageHelper.MIN_ATK, SkillDamageHelper.scaleByATK(user, pct));

        user.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.5, target.getZ(),
                    10, 0.1, 0.5, 0.1, 0.1);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.2, 0.2, 0.2, 0.1);

            var damageSource = user instanceof Player p
                    ? user.damageSources().playerAttack(p)
                    : user.damageSources().mobAttack(user);
            
            SkillDamageHelper.dealSkillDamage(target, damageSource, damage);

            // Fatal Blow stun (Lv6+ with Fatal Blow skill learned)
            if (user instanceof Player player && level >= 6) {
                SkillProgress fbProgress = SkillProgressManager.getProgress(player, FATAL_BLOW);
                if (fbProgress != null && fbProgress.getLevel() > 0) {
                    float stunChance = 0.05f * (level - 5);
                    if (user.getRandom().nextFloat() <= stunChance) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, true, true));
                        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, true, false));
                        serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT,
                                target.getX(), target.getY() + 2.2, target.getZ(),
                                8, 0.2, 0.1, 0.2, 0);
                    }
                }
            }
        }
    }
}
