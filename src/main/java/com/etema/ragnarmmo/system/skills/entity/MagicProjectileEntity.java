package com.etema.ragnarmmo.system.skills.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public class MagicProjectileEntity extends ThrowableProjectile {
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> PARTICLE_URL = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TARGET_ID = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    private ParticleOptions serverParticle = ParticleTypes.FLAME;
    private ParticleOptions secondaryParticle = null;
    private float damage = 0;
    private Consumer<EntityHitResult> onHitEffect = null;
    private LivingEntity target = null;
    private double homingStrength = 0.05;

    public MagicProjectileEntity(EntityType<? extends MagicProjectileEntity> type, Level level) {
        super(type, level);
    }

    public MagicProjectileEntity(Level level, LivingEntity owner, float damage, ParticleOptions particle) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.MAGIC_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.damage = damage;
        this.serverParticle = particle;
        this.setParticleSync(particle);
        this.setNoGravity(true);
    }

    public void setSecondaryParticle(ParticleOptions particle) {
        this.secondaryParticle = particle;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
        if (!this.level().isClientSide) {
            this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
        }
    }

    public void setHomingStrength(double strength) {
        this.homingStrength = strength;
    }

    private void setParticleSync(ParticleOptions particle) {
        if (!this.level().isClientSide) {
            this.entityData.set(PARTICLE_URL, net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getKey(particle.getType()).toString());
        }
    }

    public ParticleOptions getParticle() {
        if (!this.level().isClientSide) return serverParticle;
        
        String idStr = this.entityData.get(PARTICLE_URL);
        if (idStr.isEmpty() || idStr.equals("minecraft:flame")) return ParticleTypes.FLAME;
        
        net.minecraft.core.particles.ParticleType<?> type = net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(new net.minecraft.resources.ResourceLocation(idStr));
        return (ParticleOptions) (type instanceof ParticleOptions ? type : ParticleTypes.FLAME);
    }

    public void setOnHitEffect(Consumer<EntityHitResult> callback) {
        this.onHitEffect = callback;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARTICLE_URL, "minecraft:flame");
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // Spawn trail particles on client
            ParticleOptions currentParticle = getParticle();
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(currentParticle, 
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.3, 
                    this.getY() + (this.random.nextDouble() - 0.5) * 0.3, 
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, 
                    0, 0, 0);
                
                if (secondaryParticle != null) {
                    this.level().addParticle(secondaryParticle, 
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.4, 
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.4, 
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.4, 
                        0, 0, 0);
                }
            }
        }
        
        // Homing logic (server side primarily for physical impact, client for visuals if needed)
        if (!this.level().isClientSide && target != null && target.isAlive()) {
            net.minecraft.world.phys.Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
            net.minecraft.world.phys.Vec3 dir = targetPos.subtract(this.position()).normalize();
            net.minecraft.world.phys.Vec3 vel = this.getDeltaMovement();
            double speed = vel.length();
            
            // Nudge velocity towards target
            this.setDeltaMovement(vel.scale(1.0 - homingStrength).add(dir.scale(speed * homingStrength)));
        }
        
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity target && !this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner instanceof ServerPlayer player) {
                target.hurt(player.damageSources().indirectMagic(this, player), damage);
            } else {
                target.hurt(this.damageSources().indirectMagic(this, owner), damage);
            }
            
            if (onHitEffect != null) {
                onHitEffect.accept(result);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
