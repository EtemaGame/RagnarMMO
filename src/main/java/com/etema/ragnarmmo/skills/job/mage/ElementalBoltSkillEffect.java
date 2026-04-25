package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import com.etema.ragnarmmo.entity.projectile.FireBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.IceBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import net.minecraft.sounds.SoundEvents;

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
    public void execute(LivingEntity user, int level) {
        if (level <= 0)
            return;

        // Mobs might use the target they are already tracking
        LivingEntity target = (user instanceof Mob mob) ? mob.getTarget() : getTarget(user);
        // Allow shooting even if target is null

        // Visual and Sound metadata
        final ParticleOptions mainParticle = getParticle();
        final SoundEvent finalSound = getSound();
        // RO: Bolts deal 100% MATK each
        final float finalDamage = SkillDamageHelper.scaleByMATK(user, 100.0f);
        final int maxHits = Math.min(level, 10);

        // Initial Casting Phase (10 ticks)
        for (int t = 0; t < 10; t++) {
            SkillSequencer.schedule(t, () -> {
                if (user.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, user.getX(), user.getY() + 0.1, user.getZ(), 5, 0.4, 0.1, 0.4, 0.05);
                    sl.sendParticles(mainParticle, user.getX(), user.getY() + 1.2, user.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        for (int i = 0; i < maxHits; i++) {
            int delay = 10 + (i * 4); // Start after 10 ticks casting
            
            SkillSequencer.schedule(delay, () -> {
                if (!user.isAlive()) return;

                // RO Style: Bolts fall from the sky above the target
                LivingEntity currentTarget = (user instanceof Mob mob) ? mob.getTarget() : getTarget(user);
                Vec3 strikePos;
                if (currentTarget != null && currentTarget.isAlive()) {
                    strikePos = currentTarget.position();
                } else {
                    // If no target, strike where the player is looking
                    HitResult ray = user.pick(15.0, 0.0f, false);
                    strikePos = ray.getLocation();
                }

                // Spawn 10 blocks above the strike point
                Vec3 startPos = strikePos.add(0, 10, 0);
                
                AbstractMagicProjectile projectile = switch (elementType) {
                    case FIRE -> new FireBoltProjectile(user.level(), user, finalDamage);
                    case WATER -> new IceBoltProjectile(user.level(), user, finalDamage);
                    case WIND -> new LightningBoltProjectile(user.level(), user, finalDamage);
                };
                
                projectile.setPos(startPos.x, startPos.y, startPos.z);
                // Shoot downwards
                projectile.shoot(0, -1, 0, 1.5f, 0.0f); 
                
                user.level().addFreshEntity(projectile);
            });
        }
    }

    private ParticleOptions getParticle() {
        return switch (elementType) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case WIND -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    private SoundEvent getSound() {
        return switch (elementType) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case WIND -> SoundEvents.LIGHTNING_BOLT_THUNDER;
        };
    }

    private LivingEntity getTarget(LivingEntity user) {
        Vec3 start = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = start.add(look.scale(15.0));

        AABB searchBox = user.getBoundingBox().inflate(15.0);
        List<LivingEntity> possibleTargets = user.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive());

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
