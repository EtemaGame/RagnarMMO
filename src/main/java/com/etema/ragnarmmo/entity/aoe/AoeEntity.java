package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.entity.IVisualSkillEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * Base class for all Area of Effect entities in RagnarMMO.
 */
public abstract class AoeEntity extends Projectile implements IVisualSkillEntity {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AoeEntity.class, EntityDataSerializers.FLOAT);

    protected float damage;
    protected int duration = 100; // 5 seconds default
    protected int reapplicationDelay = 20;

    public AoeEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AoeEntity(EntityType<? extends Projectile> entityType, Level level, LivingEntity owner, float radius, float damage, int duration) {
        this(entityType, level);
        this.setOwner(owner);
        this.setRadius(radius);
        this.damage = damage;
        this.duration = duration;
    }

    public abstract void ambientParticles();

    public abstract void applyEffect(LivingEntity target);

    @Override
    public void tick() {
        super.tick();
        if (tickCount > duration) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            if (tickCount % reapplicationDelay == 0) {
                checkHits();
            }
        } else {
            ambientParticles();
        }
    }

    protected void checkHits() {
        float radius = getRadius();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 0.5, radius));
        for (LivingEntity target : targets) {
            if (canHitEntity(target) && target.distanceTo(this) <= radius) {
                applyEffect(target);
            }
        }
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    public void setRadius(float radius) {
        this.entityData.set(DATA_RADIUS, Mth.clamp(radius, 0.0F, 32.0F));
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 2.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.tickCount);
        tag.putInt("Duration", this.duration);
        tag.putFloat("Radius", this.getRadius());
        tag.putFloat("Damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("Age");
        this.duration = tag.getInt("Duration");
        this.setRadius(tag.getFloat("Radius"));
        this.damage = tag.getFloat("Damage");
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 1.0F);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
