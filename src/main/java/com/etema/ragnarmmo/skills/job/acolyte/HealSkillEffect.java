package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.skills.execution.instant.InstantTargetSkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.jetbrains.annotations.Nullable;

/**
 * Heal — Active recovery skill (Acolyte).
 * Heals target (or self), or damages Undead.
 */
public class HealSkillEffect extends InstantTargetSkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "heal");

    public HealSkillEffect() {
        super(ID);
    }

    public HealSkillEffect(ResourceLocation id) {
        super(id);
    }

    @Override
    protected double getRange(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelDouble("range", level, 5.0D))
                .orElse(5.0D);
    }

    @Override
    protected int getAnimationDelay(int level) {
        return 10;
    }

    @Override
    @Nullable
    protected LivingEntity getTarget(LivingEntity user, double range) {
        if (user instanceof net.minecraft.server.level.ServerPlayer player) {
            return AcolyteTargetingHelper.resolveHealTarget(player, range);
        }
        LivingEntity target = super.getTarget(user, range);
        return target != null ? target : user;
    }

    @Override
    protected void playInitialVisuals(LivingEntity user, @Nullable LivingEntity target, int level) {
        // Initial Casting Phase (Magic Circle)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            SkillSequencer.schedule(t, () -> {
                if (user.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    double radius_circle = 1.0;
                    for (int j = 0; j < 8; j++) {
                        double angle = (tick * 0.5) + (j * Math.PI * 2 / 8.0);
                        double dx = Math.cos(angle) * radius_circle;
                        double dz = Math.sin(angle) * radius_circle;
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, user.getX() + dx, user.getY() + 0.1, user.getZ() + dz, 1, 0, 0, 0, 0);
                    }
                }
            });
        }
    }

    @Override
    protected void applyEffect(LivingEntity user, @Nullable LivingEntity target, int level) {
        // Validation: Target should not be null (defaults to self in getTarget)
        if (target == null) target = user;

        var defOpt = SkillRegistry.get(ID);
        float healBase = defOpt
                .map(def -> (float) def.getLevelDouble("heal_base", level, 4.0D + (8.0D * level)))
                .orElse(4.0f + (8.0f * level));
        float healAmount = healBase;
        if (user instanceof net.minecraft.world.entity.player.Player player) {
            double healMultiplier = Math.max(1.0D,
                    Math.floor((SkillDamageHelper.getBaseLevel(player) + SkillDamageHelper.getINT(player)) / 8.0D));
            healAmount = (float) (healBase * healMultiplier);
        } else {
            healAmount = SkillDamageHelper.getHealAmount(user, level);
        }
        float undeadDamageRatio = defOpt
                .map(def -> (float) def.getLevelDouble("undead_damage_ratio", level, 0.5))
                .orElse(0.5f);

        if (target.getMobType() == MobType.UNDEAD) {
            // Damage undead
            target.hurt(user.damageSources().magic(), healAmount * undeadDamageRatio);
            user.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.ZOMBIE_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
            if (user.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.2, 0.5, 0.2, 0.05);
            }
        } else {
            // Heal target or self
            target.heal(healAmount);
            user.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    RagnarSounds.DILECTIO_HEAL.get(), SoundSource.PLAYERS,
                    1.0F, 1.2F);
            
            // RO Style: Rising healing aura
            if (user.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                for (int i = 0; i < 20; i++) {
                    double ox = (user.getRandom().nextDouble() - 0.5) * 0.8;
                    double oz = (user.getRandom().nextDouble() - 0.5) * 0.8;
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, target.getX() + ox, target.getY(), target.getZ() + oz, 1, 0, 0.2, 0, 0.1);
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, target.getX() + ox, target.getY() + 0.5, target.getZ() + oz, 1, 0, 0.1, 0, 0.05);
                }
                SkillVisualFx.spawnVerticalCross(sl, target.position(), 0.15, 1.6, 0.32,
                        net.minecraft.core.particles.ParticleTypes.END_ROD, net.minecraft.core.particles.ParticleTypes.GLOW);
                SkillVisualFx.spawnRing(sl, target.position(), 0.85, 0.12,
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, 10);
            }
        }
    }
}
