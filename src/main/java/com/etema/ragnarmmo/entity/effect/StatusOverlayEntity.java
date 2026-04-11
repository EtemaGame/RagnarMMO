package com.etema.ragnarmmo.entity.effect;

import com.etema.ragnarmmo.common.init.RagnarEntities;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * Lightweight visual shell that follows a target while a hard status is active.
 */
public class StatusOverlayEntity extends Entity {
    private static final double TARGET_LOOKUP_RANGE = 32.0D;

    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(StatusOverlayEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(StatusOverlayEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(StatusOverlayEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(StatusOverlayEntity.class,
            EntityDataSerializers.FLOAT);

    private int durationTicks = 60;

    public StatusOverlayEntity(EntityType<? extends StatusOverlayEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public StatusOverlayEntity(Level level) {
        this(RagnarEntities.STATUS_OVERLAY.get(), level);
    }

    public static void spawnOrRefresh(ServerLevel level, LivingEntity target, Variant variant, int durationTicks) {
        List<StatusOverlayEntity> existing = findForTarget(level, target);

        if (!existing.isEmpty()) {
            existing.get(0).refresh(target, variant, durationTicks);
            // Cleanup any duplicates that might have existed from previous versions
            for (int i = 1; i < existing.size(); i++) {
                existing.get(i).discard();
            }
            return;
        }

        StatusOverlayEntity overlay = new StatusOverlayEntity(level);
        overlay.refresh(target, variant, durationTicks);
        level.addFreshEntity(overlay);
    }

    public static void clearForTarget(Level level, LivingEntity target) {
        List<StatusOverlayEntity> overlays = findForTarget(level, target);
        for (StatusOverlayEntity overlay : overlays) {
            overlay.discard();
        }
    }

    private static List<StatusOverlayEntity> findForTarget(Level level, LivingEntity target) {
        if (level == null || target == null) {
            return List.of();
        }

        return level.getEntitiesOfClass(StatusOverlayEntity.class,
                target.getBoundingBox().inflate(TARGET_LOOKUP_RANGE),
                overlay -> overlay.isAttachedTo(target));
    }

    public void refresh(LivingEntity target, Variant variant, int durationTicks) {
        this.entityData.set(DATA_TARGET_ID, target.getId());
        this.entityData.set(DATA_VARIANT, variant.id);
        this.entityData.set(DATA_WIDTH, Math.max(0.8f, target.getBbWidth() * 1.2f));
        this.entityData.set(DATA_HEIGHT, Math.max(0.9f, target.getBbHeight() * 1.05f));
        this.durationTicks = Math.max(10, durationTicks);
        this.tickCount = 0;
        this.setPos(target.getX(), target.getY(), target.getZ());
    }

    public Variant getVariant() {
        return Variant.fromId(this.entityData.get(DATA_VARIANT));
    }

    public float getVisualWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getVisualHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public LivingEntity getAttachedTarget() {
        Entity entity = level().getEntity(this.entityData.get(DATA_TARGET_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isAttachedTo(LivingEntity target) {
        return target != null && this.entityData.get(DATA_TARGET_ID) == target.getId();
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = getAttachedTarget();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }

        if (!level().isClientSide && tickCount > durationTicks) {
            discard();
            return;
        }

        setPos(target.getX(), target.getY(), target.getZ());
        setYRot(target.getYRot());

        if (level() instanceof ServerLevel serverLevel && tickCount % 6 == 0) {
            Variant variant = getVariant();
            SkillVisualFx.spawnBlockBurst(serverLevel, target, variant.outerState, 6, 0.18, target.getBbHeight() * 0.35, 0.02);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, variant.innerState),
                    target.getX(), target.getY() + (target.getBbHeight() * 0.75), target.getZ(),
                    4, 0.12, 0.12, 0.12, 0.01);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_VARIANT, Variant.FROZEN.id);
        this.entityData.define(DATA_WIDTH, 1.0f);
        this.entityData.define(DATA_HEIGHT, 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.durationTicks = tag.getInt("Duration");
        this.entityData.set(DATA_TARGET_ID, tag.getInt("TargetId"));
        this.entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        this.entityData.set(DATA_WIDTH, tag.getFloat("VisualWidth"));
        this.entityData.set(DATA_HEIGHT, tag.getFloat("VisualHeight"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Duration", this.durationTicks);
        tag.putInt("TargetId", this.entityData.get(DATA_TARGET_ID));
        tag.putInt("Variant", this.entityData.get(DATA_VARIANT));
        tag.putFloat("VisualWidth", this.entityData.get(DATA_WIDTH));
        tag.putFloat("VisualHeight", this.entityData.get(DATA_HEIGHT));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public enum Variant {
        FROZEN(0, Blocks.BLUE_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState()),
        STONE(1, Blocks.STONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState());

        private final int id;
        private final BlockState outerState;
        private final BlockState innerState;

        Variant(int id, BlockState outerState, BlockState innerState) {
            this.id = id;
            this.outerState = outerState;
            this.innerState = innerState;
        }

        public static Variant fromId(int id) {
            for (Variant variant : values()) {
                if (variant.id == id) {
                    return variant;
                }
            }
            return FROZEN;
        }

        public BlockState outerState() {
            return outerState;
        }

        public BlockState innerState() {
            return innerState;
        }
    }
}
