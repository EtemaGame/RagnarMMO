package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.skills.execution.instant.InstantTargetSkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.data.progression.SkillProgressManager;
import com.etema.ragnarmmo.skills.data.progression.SkillProgress;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "bash");
    private static final ResourceLocation FATAL_BLOW = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fatal_blow");

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
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
