package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireWallAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_wall");

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }
    
    private int hitsLeft;

    public FireWallAoe(EntityType<? extends FireWallAoe> type, Level level) {
        super(type, level);
    }

    public FireWallAoe(Level level, LivingEntity owner, float radius, float damage, int duration, int maxHits) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.FIRE_WALL_AOE.get(), level, owner, radius, damage, duration);
        this.reapplicationDelay = 2; // Hit very fast
        this.hitsLeft = maxHits;
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // "All monsters except Undead take 50% damage... and are knocked back"
        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            return;
        }

        if (hitsLeft > 0) {
            SkillDamageHelper.dealSkillDamage(target, this.damageSources().inFire(), this.damage);
            
            // Knockback away from the pillar center
            Vec3 knockback = target.position().subtract(this.position()).normalize().scale(0.5);
            target.push(knockback.x, 0.1, knockback.z);
            
            hitsLeft--;
            if (hitsLeft <= 0) {
                discard();
            }
            
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
    }

    @Override
    public void ambientParticles() {
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 8; i++) {
                double px = getX() + (random.nextDouble() - 0.5) * 0.55;
                double py = getY() + 0.15 + random.nextDouble() * 1.6;
                double pz = getZ() + (random.nextDouble() - 0.5) * 0.55;
                sl.sendParticles(ParticleTypes.SMALL_FLAME, px, py, pz, 1, 0.03, 0.08, 0.03, 0.0);
                if (random.nextBoolean()) {
                    sl.sendParticles(ParticleTypes.SMOKE, px, py + 0.1, pz, 1, 0.02, 0.05, 0.02, 0.0);
                }
                if (random.nextInt(4) == 0) {
                    sl.sendParticles(ParticleTypes.LAVA, px, getY() + 0.08, pz, 1, 0.01, 0.0, 0.01, 0.0);
                }
            }
            if (tickCount % 14 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 0.25f, 1.2f);
            }
        }
    }
}
