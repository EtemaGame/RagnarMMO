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
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
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
