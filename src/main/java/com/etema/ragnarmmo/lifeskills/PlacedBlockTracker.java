package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Tracks blocks placed by players to prevent exploit farming.
 * Only naturally generated blocks award Life Skill points.
 *
 * Storage: Per-dimension SavedData with chunk-based organization.
 * Cleanup: Positions are removed when blocks are broken.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class PlacedBlockTracker extends SavedData {

    private static final String DATA_NAME = "ragnarmmo_placed_blocks";

    // Map of ChunkPos -> Set of relative block positions within chunk
    // Using long encoding for positions to save memory
    private final Map<Long, Set<Long>> placedBlocks = new HashMap<>();

    // Maximum positions per chunk (memory limit)
    private static final int MAX_PER_CHUNK = 4096;

    public PlacedBlockTracker() {
    }

    public static PlacedBlockTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlacedBlockTracker::load,
                PlacedBlockTracker::new,
                DATA_NAME);
    }

    /**
     * Mark a block position as player-placed.
     */
    public void markPlaced(BlockPos pos) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<Long> positions = placedBlocks.computeIfAbsent(chunkKey, k -> new HashSet<>());

        // Limit positions per chunk to prevent memory issues
        if (positions.size() < MAX_PER_CHUNK) {
            positions.add(pos.asLong());
            setDirty();
        }
    }

    /**
     * Remove a block position from tracking (when broken).
     */
    public void unmarkPlaced(BlockPos pos) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<Long> positions = placedBlocks.get(chunkKey);
        if (positions != null) {
            if (positions.remove(pos.asLong())) {
                setDirty();
                // Clean up empty chunks
                if (positions.isEmpty()) {
                    placedBlocks.remove(chunkKey);
                }
            }
        }
    }

    /**
     * Check if a block was placed by a player.
     */
    public boolean isPlayerPlaced(BlockPos pos) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<Long> positions = placedBlocks.get(chunkKey);
        return positions != null && positions.contains(pos.asLong());
    }

    /**
     * Check if a block is natural (not player-placed).
     */
    public boolean isNaturalBlock(BlockPos pos) {
        return !isPlayerPlaced(pos);
    }

    // === Event Handlers ===

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Only track if placed by a player
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                PlacedBlockTracker tracker = get(serverLevel);
                tracker.markPlaced(event.getPos());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PlacedBlockTracker tracker = get(serverLevel);
            // Always try to unmark - handles case where it was placed
            tracker.unmarkPlaced(event.getPos());
        }
    }

    // === NBT Serialization ===

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag chunkList = new ListTag();

        for (Map.Entry<Long, Set<Long>> entry : placedBlocks.entrySet()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("chunk", entry.getKey());

            long[] positions = entry.getValue().stream().mapToLong(Long::longValue).toArray();
            chunkTag.putLongArray("positions", positions);

            chunkList.add(chunkTag);
        }

        tag.put("chunks", chunkList);
        return tag;
    }

    public static PlacedBlockTracker load(CompoundTag tag) {
        PlacedBlockTracker tracker = new PlacedBlockTracker();

        ListTag chunkList = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkList.size(); i++) {
            CompoundTag chunkTag = chunkList.getCompound(i);
            long chunkKey = chunkTag.getLong("chunk");
            long[] positions = chunkTag.getLongArray("positions");

            Set<Long> posSet = new HashSet<>();
            for (long pos : positions) {
                posSet.add(pos);
            }

            if (!posSet.isEmpty()) {
                tracker.placedBlocks.put(chunkKey, posSet);
            }
        }

        return tracker;
    }

    /**
     * Get statistics for debugging/admin commands.
     */
    public int getTotalTrackedBlocks() {
        return placedBlocks.values().stream().mapToInt(Set::size).sum();
    }

    public int getTrackedChunkCount() {
        return placedBlocks.size();
    }
}
