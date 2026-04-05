package com.etema.ragnarmmo.entity.projectile;

import com.etema.ragnarmmo.entity.IVisualSkillEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public class MagicProjectileEntity extends ThrowableProjectile implements IVisualSkillEntity {
    private ResourceLocation skillId = new ResourceLocation("ragnarmmo", "magic_projectile");

    public void setSkillId(ResourceLocation skillId) {
        this.skillId = skillId;
    }

    @Override
    public ResourceLocation getSkillId() {
        return skillId;
    }

    @Override
    public void ambientParticles() {
        // No default ambient particles for the base magic projectile
    }

    private static final net.minecraft.network.syncher.EntityDataAccessor<String> PARTICLE_URL = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TARGET_ID = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> PROJECTILE_TYPE = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HOMING = 
        net.minecraft.network.syncher.SynchedEntityData.defineId(MagicProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private ParticleOptions serverParticle = ParticleTypes.FLAME;
    private ParticleOptions secondaryParticle = null;
    private float damage = 0;
    private Consumer<EntityHitResult> onHitEffect = null;
    private LivingEntity target = null;
    private double homingStrength = 0.05;
    private float gravity = 0.0f;

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

    public void setGravity(float gravity) {
        this.gravity = gravity;
        this.setNoGravity(gravity == 0);
    }

    public void setHoming(boolean homing) {
        if (!this.level().isClientSide) {
            this.entityData.set(HOMING, homing);
        }
    }

    public boolean isHoming() {
        return this.entityData.get(HOMING);
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

    public void setProjectileType(String type) {
        if (!this.level().isClientSide) {
            this.entityData.set(PROJECTILE_TYPE, type);
        }
    }

    public String getProjectileType() {
        return this.entityData.get(PROJECTILE_TYPE);
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
        
        ResourceLocation resLoc = ResourceLocation.tryParse(idStr);
        if (resLoc == null) return ParticleTypes.FLAME;
        net.minecraft.core.particles.ParticleType<?> type = net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(resLoc);
        return (ParticleOptions) (type instanceof ParticleOptions ? type : ParticleTypes.FLAME);
    }

    public void setOnHitEffect(Consumer<EntityHitResult> callback) {
        this.onHitEffect = callback;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARTICLE_URL, "minecraft:flame");
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(HOMING, false);
        this.entityData.define(PROJECTILE_TYPE, "default");
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            // Extra particles removed as requested. Projectile is now represented by its 3D model.
        }
        
        if (!this.level().isClientSide && isHoming() && target != null && target.isAlive()) {
            net.minecraft.world.phys.Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
            net.minecraft.world.phys.Vec3 dir = targetPos.subtract(this.position()).normalize();
            net.minecraft.world.phys.Vec3 vel = this.getDeltaMovement();
            double speed = vel.length();
            
            this.setDeltaMovement(vel.scale(1.0 - homingStrength).add(dir.scale(speed * homingStrength)));
        }

        if (!this.isNoGravity()) {
            net.minecraft.world.phys.Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x, vel.y - gravity, vel.z);
        }

        net.minecraft.world.phys.Vec3 motion = this.getDeltaMovement();
        double horizontalDist = motion.horizontalDistance();
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * (180D / Math.PI)));
        this.setXRot((float) (Math.atan2(motion.y, horizontalDist) * (180D / Math.PI)));

        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            super.onHit(result);
            if (!this.level().isClientSide) {
                if (this.onHitEffect != null) {
                    this.onHitEffect.accept(null);
                }
                this.discard();
            }
        } else {
            super.onHit(result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity victim) {
            LivingEntity attacker = (LivingEntity) this.getOwner();
            if (attacker != null) {
                boolean isMagic = "fireball".equals(getProjectileType()) || "bolt".equals(getProjectileType());
                
                if (!isMagic) {
                    double hit = getEntityHit(attacker);
                    double flee = getEntityFlee(victim);
                    double hitRate = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeHitRate(hit, flee);
                    
                    if (!com.etema.ragnarmmo.system.stats.compute.CombatMath.rollHit(hitRate, this.random)) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.5f);
                        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, victim.getX(), victim.getY() + 1, victim.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                        }
                        
                        if (this.onHitEffect != null) {
                            this.onHitEffect.accept(result);
                        }
                        
                        this.discard();
                        return;
                    }
                }
            }
            
            Entity owner = this.getOwner();
            if (owner instanceof ServerPlayer player) {
                victim.hurt(player.damageSources().indirectMagic(this, player), damage);
            } else {
                victim.hurt(this.damageSources().indirectMagic(this, owner), damage);
            }
            
            if (this.onHitEffect != null) {
                this.onHitEffect.accept(result);
            }
        }
        this.discard();
    }

    private double getEntityHit(LivingEntity entity) {
        int dex = (int) getSafeAttributeValue(entity, com.etema.ragnarmmo.common.api.stats.StatKeys.DEX);
        int luk = (int) getSafeAttributeValue(entity, com.etema.ragnarmmo.common.api.stats.StatKeys.LUK);
        
        final int[] levelArr = {1};
        if (entity instanceof Player p) {
            p.getCapability(com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider.CAP).ifPresent(stats -> levelArr[0] = stats.getLevel());
        }
        return com.etema.ragnarmmo.system.stats.compute.CombatMath.computeHIT(dex, luk, levelArr[0], 0);
    }

    private double getEntityFlee(LivingEntity entity) {
        int agi = (int) getSafeAttributeValue(entity, com.etema.ragnarmmo.common.api.stats.StatKeys.AGI);
        int luk = (int) getSafeAttributeValue(entity, com.etema.ragnarmmo.common.api.stats.StatKeys.LUK);
        
        final int[] levelArr = {1};
        if (entity instanceof Player p) {
            p.getCapability(com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider.CAP).ifPresent(stats -> levelArr[0] = stats.getLevel());
        }
        return com.etema.ragnarmmo.system.stats.compute.CombatMath.computeFLEE(agi, luk, levelArr[0], 0);
    }

    private double getSafeAttributeValue(LivingEntity entity, com.etema.ragnarmmo.common.api.stats.StatKeys key) {
        net.minecraft.world.entity.ai.attributes.Attribute attr = com.etema.ragnarmmo.common.api.stats.StatAttributes.getAttribute(key);
        if (attr == null) return 1.0;
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttribute(attr);
        return instance != null ? instance.getValue() : 1.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
