package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ElementalBoltSkillEffect implements ISkillEffect {

    private final ResourceLocation id;
    private final ElementType elementType;

    public enum ElementType {
        FIRE, WATER, WIND
    }

    public ElementalBoltSkillEffect(ResourceLocation id, ElementType elementType) {
        this.id = id;
        this.elementType = elementType;
    }

    @Override
    public ResourceLocation getSkillId() {
        return id;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getTarget(player);
        if (target == null || target == player)
            return;

        // Visual and Sound metadata
        final ParticleOptions mainParticle = getParticle();
        final SoundEvent finalSound = getSound();
        final float finalDamage = 4.0f; // Damage per bolt
        final int maxHits = Math.min(level, 10);

        // Initial Casting Phase (10 ticks)
        for (int t = 0; t < 10; t++) {
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 5, 0.4, 0.1, 0.4, 0.05);
                    sl.sendParticles(mainParticle, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        for (int i = 0; i < maxHits; i++) {
            int delay = 10 + (i * 4); // Start after 10 ticks casting
            
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(delay, () -> {
                if (!target.isAlive()) return;

                Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
                Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2.0, 0);
                Vec3 shootDir = targetVec.subtract(startPos).normalize();

                com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity projectile = 
                    new com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity(player.level(), player, finalDamage, mainParticle);
                
                // Element specific secondary trail
                ParticleOptions secondary = switch (elementType) {
                    case FIRE -> net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA;
                    case WATER -> net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER;
                    case WIND -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
                };
                projectile.setSecondaryParticle(secondary);
                projectile.setTarget(target);
                projectile.setHomingStrength(0.15); // Bolts are light and fast
                
                projectile.setPos(startPos.x, startPos.y, startPos.z);
                projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.4f, 2.0f); // Fast with slight spread
                
                projectile.setOnHitEffect(result -> {
                    player.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                            finalSound, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.3f + (float)Math.random() * 0.4f);

                    if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                result.getLocation().x, result.getLocation().y, result.getLocation().z, 1, 0.1, 0.1, 0.1, 0.0);
                        sl.sendParticles(mainParticle, result.getLocation().x, result.getLocation().y, result.getLocation().z, 15, 0.2, 0.2, 0.2, 0.05);
                    }
                });

                player.level().addFreshEntity(projectile);
            });
        }
    }

    private ParticleOptions getParticle() {
        return switch (elementType) {
            case FIRE -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case WATER -> net.minecraft.core.particles.ParticleTypes.SPLASH;
            case WIND -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
        };
    }

    private SoundEvent getSound() {
        return switch (elementType) {
            case FIRE -> net.minecraft.sounds.SoundEvents.FIRECHARGE_USE;
            case WATER -> net.minecraft.sounds.SoundEvents.PLAYER_SPLASH;
            case WIND -> net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER;
        };
    }

    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(15.0));

        AABB searchBox = player.getBoundingBox().inflate(15.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent()) {
                double dist = start.distanceToSqr(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }

        return closestTarget;
    }
}
