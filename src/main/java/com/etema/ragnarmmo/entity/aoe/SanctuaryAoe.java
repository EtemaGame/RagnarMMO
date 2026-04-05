package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;

/**
 * Sanctuary AoE — Heals allies and damages Undead/Demons.
 */
public class SanctuaryAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = new ResourceLocation("ragnarmmo", "sanctuary");

    private float healAmount;

    public SanctuaryAoe(EntityType<? extends SanctuaryAoe> type, Level level) {
        super(type, level);
    }

    public SanctuaryAoe(Level level, LivingEntity owner, float radius, float damage, float healAmount, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.SANCTUARY_AOE.get(), level, owner, radius, damage, duration);
        this.healAmount = healAmount;
        this.reapplicationDelay = 20; // Every 1 second
    }

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (target.getMobType() == MobType.UNDEAD) {
            SkillDamageHelper.dealSkillDamage(target, this.damageSources().indirectMagic(this, getOwner()), this.damage);
        } else {
            target.heal(this.healAmount);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0.2, 0, 0.0);
            }
        }
    }

    @Override
    public void ambientParticles() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 20, getRadius(), 0.1, getRadius(), 0.05);
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY(), getZ(), 5, getRadius(), 0.5, getRadius(), 0.01);
            
            if (tickCount % 20 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }
    }
}
