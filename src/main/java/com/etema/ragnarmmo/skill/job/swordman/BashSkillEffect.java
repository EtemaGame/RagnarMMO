package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.skill.execution.instant.InstantTargetSkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.data.progression.SkillProgressManager;
import com.etema.ragnarmmo.skill.data.progression.SkillProgress;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Bash — Active melee skill (Swordman).
 * RO Formula: Single hit dealing (100 + 30 x level)% ATK.
 * Lv6+ with Fatal Blow skill learned has a (5 × (Lv - 5))% stun chance.
 */
public class BashSkillEffect extends InstantTargetSkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "bash");
    private static final ResourceLocation FATAL_BLOW = new ResourceLocation("ragnarmmo", "fatal_blow");

    public BashSkillEffect() {
        super(ID);
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
        user.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        if (user.level().isClientSide) return;
        
        // Sweep flash at the moment of activation (swing)
        if (user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    user.getX() + user.getLookAngle().x,
                    user.getY() + 1.2,
                    user.getZ() + user.getLookAngle().z,
                    1, 0, 0, 0, 0);
            SkillVisualFx.spawnFrontArc(serverLevel, user, 1.5, 1.4, 1.1,
                    ParticleTypes.CRIT, ParticleTypes.SWEEP_ATTACK, 7);
        }
    }

    @Override
    protected void applyEffect(LivingEntity user, @Nullable LivingEntity target, int level) {
        if (target == null || !target.isAlive()) return;

        if (!(user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        var defOpt = SkillRegistry.get(ID);
        double accuracyBonus = defOpt
                .map(def -> def.getLevelDouble("accuracy_bonus", level, level * 5.0))
                .orElse(level * 5.0);

        if (!SwordmanCombatUtil.rollPhysicalSkillHit(user, target, accuracyBonus, 1.0)) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + 1.1, target.getZ(),
                    6, 0.15, 0.2, 0.15, 0.01);
            return;
        }

        float pct = defOpt
                .map(def -> (float) def.getLevelDouble("damage_percent", level, 100.0 + (30.0 * level)))
                .orElse(100.0f + (30.0f * level));
        float damage = Math.max(SkillDamageHelper.MIN_ATK, SkillDamageHelper.scaleByATK(user, pct));

        user.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                com.etema.ragnarmmo.common.init.RagnarSounds.BASH.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1.5, target.getZ(),
                10, 0.1, 0.5, 0.1, 0.1);
        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                target.getX(), target.getY() + 1.0, target.getZ(),
                15, 0.2, 0.2, 0.2, 0.1);
        SkillVisualFx.spawnFrontArc(serverLevel, user, 1.8, 1.6, 1.0,
                ParticleTypes.ENCHANTED_HIT, ParticleTypes.CRIT, 9);

        var damageSource = user instanceof Player p
                ? user.damageSources().playerAttack(p)
                : user.damageSources().mobAttack(user);

        SwordmanCombatUtil.withSkillDamageContext(user,
                () -> SkillDamageHelper.dealSkillDamage(target, damageSource, damage));

        // Fatal Blow stun (Lv6+ with Fatal Blow skill learned)
        if (user instanceof Player player && level >= 6) {
            SkillProgress fbProgress = SkillProgressManager.getProgress(player, FATAL_BLOW);
            if (fbProgress != null && fbProgress.getLevel() > 0) {
                float baseStunChance = defOpt
                        .map(def -> (float) def.getLevelDouble("stun_chance_percent", level, 5.0 * (level - 5)))
                        .orElse(5.0f * (level - 5)) / 100.0f;
                
                float finalStunChance = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStunChance(baseStunChance, target);
                int finalStunDuration = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStunDuration(60, target);

                if (finalStunDuration > 0 && user.getRandom().nextFloat() < finalStunChance) {
                    long until = serverLevel.getGameTime() + finalStunDuration;
                    target.getPersistentData().putLong("ragnarmmo_stunned_until", until);

                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, finalStunDuration, 4, false, true, true));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, finalStunDuration, 1, false, true, false));
                    serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT,
                            target.getX(), target.getY() + 2.2, target.getZ(),
                            8, 0.2, 0.1, 0.2, 0);
                }
            }
        }
    }
}
