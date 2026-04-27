package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.skills.runtime.SkillManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class WarpPortalHelper {

    public static final int MAX_PORTAL_USES = 8;
    public static final float PORTAL_RADIUS = 1.35f;

    private WarpPortalHelper() {
    }

    public record WarpDestination(String displayName, ResourceKey<Level> dimension, BlockPos pos) {
    }

    public record DestinationResolution(WarpDestination destination, @Nullable Component notice) {
    }

    public static DestinationResolution resolveDestination(ServerPlayer player, SkillManager skills, int skillLevel) {
        int selected = skills.getSelectedWarpDestination();
        if (selected <= 0) {
            return new DestinationResolution(resolveSavePoint(player), null);
        }

        int maxAvailableMemo = Math.max(0, Math.min(SkillManager.getMaxWarpMemos(), skillLevel - 1));
        if (selected > maxAvailableMemo) {
            String allowed = maxAvailableMemo <= 0
                    ? "solo puede usar Save Point"
                    : "solo puede usar Save Point y hasta Memo " + maxAvailableMemo;
            return new DestinationResolution(resolveSavePoint(player),
                    Component.literal("§7Warp Portal Lv." + skillLevel + " " + allowed
                            + ". Se usó Save Point."));
        }

        return skills.getWarpMemo(selected)
                .map(memo -> new DestinationResolution(toDestination(selected, memo), null))
                .orElseGet(() -> new DestinationResolution(resolveSavePoint(player),
                        Component.literal("§7Memo " + selected + " no está guardado. Se usó Save Point.")));
    }

    public static WarpDestination resolveSavePoint(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ResourceKey<Level> dimensionKey = Level.OVERWORLD;
        BlockPos pos = null;

        if (player.getRespawnPosition() != null) {
            pos = player.getRespawnPosition();
            dimensionKey = player.getRespawnDimension();
        }

        ServerLevel level = server != null ? server.getLevel(dimensionKey) : null;
        if (level == null && server != null) {
            level = server.overworld();
        }
        if (level == null) {
            level = player.serverLevel();
        }

        if (pos == null) {
            pos = level.getSharedSpawnPos();
        }

        return new WarpDestination("Save Point", level.dimension(), pos);
    }

    public static WarpDestination toDestination(int slot, SkillManager.WarpMemo memo) {
        return new WarpDestination("Memo " + slot, ResourceKey.create(Registries.DIMENSION, memo.getDimensionId()),
                memo.getPos());
    }

    public static Component describeSavePoint(ServerPlayer player) {
        return describeDestination(resolveSavePoint(player));
    }

    public static Component describeMemo(int slot, SkillManager.WarpMemo memo) {
        return describeDestination(toDestination(slot, memo));
    }

    public static Component describeDestination(WarpDestination destination) {
        BlockPos pos = destination.pos();
        String dim = destination.dimension().location().toString();
        return Component.literal(destination.displayName() + " §8→ §7" + dim + " §f(" + pos.getX() + ", " + pos.getY()
                + ", " + pos.getZ() + ")");
    }

    public static boolean teleport(ServerPlayer player, WarpDestination destination) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerLevel destinationLevel = server.getLevel(destination.dimension());
        if (destinationLevel == null) {
            return false;
        }

        Vec3 safePos = findSafeStandPosition(destinationLevel, destination.pos());
        if (safePos == null) {
            safePos = Vec3.atBottomCenterOf(destinationLevel.getSharedSpawnPos());
        }

        player.teleportTo(destinationLevel, safePos.x, safePos.y, safePos.z, Collections.<RelativeMovement>emptySet(),
                player.getYRot(), player.getXRot());
        return true;
    }

    @Nullable
    private static Vec3 findSafeStandPosition(ServerLevel level, BlockPos origin) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int dy = 3; dy >= -3; dy--) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (canStandAt(level, candidate)) {
                            return Vec3.atBottomCenterOf(candidate);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir() || !belowState.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        AABB playerBox = new AABB(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                pos.getX() + 0.9, pos.getY() + 1.9, pos.getZ() + 0.9);
        return level.noCollision(playerBox);
    }
}
