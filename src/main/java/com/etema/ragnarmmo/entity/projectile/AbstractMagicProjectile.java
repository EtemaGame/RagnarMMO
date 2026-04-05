package com.etema.ragnarmmo.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base class for all skill-based magical projectiles.
 */
public class AbstractMagicProjectile extends Projectile {

    protected UUID ownerUUID;
    protected float damage;
    protected int lifeTicks;
    protected int maxLifeTicks = 100;
    protected Entity target;
    protected double homingStrength = 0.0;
    protected String projectileType;
    protected Consumer<HitResult> onHitEffect;

    protected AbstractMagicProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    protected AbstractMagicProjectile(EntityType<? extends Projectile> type, Level level, LivingEntity owner, float damage) {
        this(type, level);
        this.ownerUUID = owner.getUUID();
        this.setOwner(owner);
        this.damage = damage;
    }

    // New constructor for data-driven skills
    protected AbstractMagicProjectile(EntityType<? extends Projectile> type, Level level, LivingEntity owner, ResourceLocation skillId, int levelValue, double damage) {
        this(type, level, owner, (float) damage);
    }

    // Generic constructor for simple skill effects
    public AbstractMagicProjectile(Level level, LivingEntity owner, float damage, net.minecraft.core.particles.ParticleOptions particle) {
        this(com.etema.ragnarmmo.common.init.RagnarEntities.MAGIC_PROJECTILE.get(), level, owner, damage);
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    public void setHoming(double strength) {
        this.homingStrength = strength;
    }

    public void setHoming(boolean enabled) {
        this.homingStrength = enabled ? 0.1 : 0.0;
    }

    public void setSecondaryParticle(net.minecraft.core.particles.SimpleParticleType particle) {
        // Placeholder or logic for secondary particles
    }

    public void shoot(Vec3 dir) {
        float speed = getSpeed();
        this.shoot(dir.x, dir.y, dir.z, speed, 0.0f);
    }

    public void setMaxLife(int ticks) {
        this.maxLifeTicks = ticks;
    }

    public void setProjectileType(String type) {
        this.projectileType = type;
    }

    public void setOnHitEffect(Consumer<HitResult> effect) {
        this.onHitEffect = effect;
    }

    public void setGravity(float gravity) {
        this.setNoGravity(gravity <= 0.0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            trailParticles();
        } else {
            lifeTicks++;
            if (lifeTicks > maxLifeTicks) {
                discard();
                return;
            }

            if (target != null && !target.isRemoved() && homingStrength > 0) {
                updateHoming();
            }
        }

        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        checkInsideBlocks();

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable());
        if (hitResult.getType() != HitResult.Type.MISS) {
            onHit(hitResult);
        }
    }

    private void updateHoming() {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
        Vec3 toTarget = targetPos.subtract(position()).normalize();
        Vec3 currentMov = getDeltaMovement().normalize();
        float speed = getSpeed();
        
        Vec3 newMov = currentMov.scale(1.0 - homingStrength).add(toTarget.scale(homingStrength)).normalize().scale(speed);
        setDeltaMovement(newMov);
    }

    public void trailParticles() {}
    public void impactParticles(double x, double y, double z) {}
    public float getSpeed() { return 1.0f; }
    public Optional<net.minecraft.sounds.SoundEvent> getImpactSound() { return Optional.empty(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (onHitEffect != null) {
            onHitEffect.accept(result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            Entity hit = result.getEntity();
            if (hit instanceof LivingEntity living && !hit.getUUID().equals(ownerUUID)) {
                impactParticles(hit.getX(), hit.getY() + hit.getBbHeight() / 2.0, hit.getZ());
                applyEffect(living);
                discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            Vec3 pos = result.getLocation();
            impactParticles(pos.x, pos.y, pos.z);
            discard();
        }
    }

    protected void applyEffect(LivingEntity target) {
        // Default damage application
        com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, 
            target.level().damageSources().magic(), damage);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        damage = tag.getFloat("Damage");
        lifeTicks = tag.getInt("LifeTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putFloat("Damage", damage);
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // Compat getters
    public float getDamage() { return damage; }
    public ResourceLocation getSkillId() { return null; } // Optional: add if needed
    public int getSkillLevel() { return 0; }
}
