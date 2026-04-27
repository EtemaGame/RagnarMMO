package com.etema.ragnarmmo.entity.aoe;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class PneumaAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "pneuma");

    public PneumaAoe(EntityType<? extends PneumaAoe> type, Level level) {
        super(type, level);
        this.reapplicationDelay = Integer.MAX_VALUE;
    }

    public PneumaAoe(Level level, LivingEntity owner, float radius, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.PNEUMA_AOE.get(), level, owner, radius, 0.0f, duration);
        this.reapplicationDelay = Integer.MAX_VALUE;
    }

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel && tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.05, getZ(),
                    8, getRadius() * 0.55, 0.02, getRadius() * 0.55, 0.01);
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.35, getZ(),
                    6, getRadius() * 0.6, 0.08, getRadius() * 0.6, 0.01);
            if (tickCount % 40 == 0) {
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45f, 1.5f);
            }
        }
    }

    @Override
    protected void checkHits() {
        // Pneuma is handled by the projectile-damage event hook.
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return false;
    }

    public boolean protects(LivingEntity target) {
        double radius = getRadius() + Math.max(0.35f, target.getBbWidth() * 0.5f);
        return target.isAlive()
                && target.level() == level()
                && target.position().distanceToSqr(position()) <= radius * radius;
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // Protection is resolved in the event hook, not by periodic reapplication.
    }

    @Override
    public void ambientParticles() {
        // Persistent visuals come from skill_visuals data + server particles in tick().
    }
}
