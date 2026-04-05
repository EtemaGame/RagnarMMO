package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.skill.job.acolyte.WarpPortalHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WarpPortalAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = new ResourceLocation("ragnarmmo", "warp_portal");

    private ResourceLocation destinationDimensionId = Level.OVERWORLD.location();
    private int destinationX;
    private int destinationY;
    private int destinationZ;
    private String destinationName = "Save Point";
    private int usesLeft = WarpPortalHelper.MAX_PORTAL_USES;
    private Set<UUID> playersInside = new HashSet<>();

    public WarpPortalAoe(EntityType<? extends WarpPortalAoe> type, Level level) {
        super(type, level);
        this.reapplicationDelay = 1;
    }

    public WarpPortalAoe(Level level, LivingEntity owner, float radius, int duration,
            WarpPortalHelper.WarpDestination destination) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.WARP_PORTAL_AOE.get(), level, owner, radius, 0.0f,
                duration);
        this.reapplicationDelay = 1;
        this.destinationDimensionId = destination.dimension().location();
        this.destinationX = destination.pos().getX();
        this.destinationY = destination.pos().getY();
        this.destinationZ = destination.pos().getZ();
        this.destinationName = destination.displayName();
    }

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (!(target instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                destinationDimensionId);
        WarpPortalHelper.WarpDestination destination = new WarpPortalHelper.WarpDestination(destinationName,
                dimensionKey,
                new net.minecraft.core.BlockPos(destinationX, destinationY, destinationZ));

        if (WarpPortalHelper.teleport(player, destination)) {
            usesLeft--;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.etema.ragnarmmo.common.init.RagnarSounds.WARP_PORTAL.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
            player.sendSystemMessage(Component.literal("§5Warp Portal §f→ §d" + destinationName));

            if (usesLeft <= 0) {
                discard();
            }
        } else {
            player.sendSystemMessage(Component.literal("§cWarp Portal: §fno se pudo encontrar el destino."));
        }
    }

    @Override
    protected void checkHits() {
        List<ServerPlayer> players = level().getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(getRadius(), 1.0, getRadius()),
                p -> p.isAlive() && p.distanceTo(this) <= getRadius());

        Set<UUID> currentInside = new HashSet<>();
        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            currentInside.add(uuid);
            if (!playersInside.contains(uuid)) {
                applyEffect(player);
                if (!isAlive()) {
                    return;
                }
            }
        }

        playersInside = currentInside;
    }

    @Override
    public void ambientParticles() {
        // Visual rendering is handled by UniversalSkillRenderer + skill_visuals data.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("DestinationDimension", destinationDimensionId.toString());
        tag.putInt("DestinationX", destinationX);
        tag.putInt("DestinationY", destinationY);
        tag.putInt("DestinationZ", destinationZ);
        tag.putString("DestinationName", destinationName);
        tag.putInt("UsesLeft", usesLeft);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("DestinationDimension"));
        destinationDimensionId = parsed != null ? parsed : Level.OVERWORLD.location();
        destinationX = tag.getInt("DestinationX");
        destinationY = tag.getInt("DestinationY");
        destinationZ = tag.getInt("DestinationZ");
        destinationName = tag.contains("DestinationName") ? tag.getString("DestinationName") : "Save Point";
        usesLeft = tag.contains("UsesLeft") ? tag.getInt("UsesLeft") : WarpPortalHelper.MAX_PORTAL_USES;
        playersInside = new HashSet<>();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return target instanceof ServerPlayer;
    }
}
