package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;

/**
 * Tracks exploration progress for a player.
 * Points are awarded for:
 * - First visit to new regions (groups of chunks)
 * - Discovering structures
 * - Long expeditions (visiting many new chunks without returning)
 *
 * Anti-exploit: Walking in circles doesn't give points.
 */
public class ExplorationTracker {

    // Region size in chunks (4x4 = 16 chunks per region)
    private static final int REGION_SIZE = 4;

    // Points per discovery type
    private static final int POINTS_NEW_REGION = 2;
    private static final int POINTS_STRUCTURE = 25;
    private static final int POINTS_EXPEDITION_BONUS = 5; // Per 10 new chunks in expedition

    // Per-dimension explored regions (encoded as long for memory efficiency)
    private final Map<ResourceKey<Level>, Set<Long>> exploredRegions = new HashMap<>();

    // Per-dimension discovered structures
    private final Map<ResourceKey<Level>, Set<String>> discoveredStructures = new HashMap<>();

    // Current expedition tracking
    private int currentExpeditionChunks = 0;
    private long lastChunkPos = Long.MIN_VALUE;
    private ResourceKey<Level> lastDimension = null;

    // Cooldown to prevent rapid chunk spam
    private long lastRegionTime = 0;
    private static final long REGION_COOLDOWN_MS = 5000; // 5 seconds between region discoveries

    public ExplorationTracker() {
    }

    /**
     * Called when player moves to a new chunk.
     * Returns points earned (0 if already explored or on cooldown).
     */
    public int onChunkEnter(ServerPlayer player, ChunkPos chunkPos, ResourceKey<Level> dimension) {
        long regionKey = getRegionKey(chunkPos);
        long now = System.currentTimeMillis();

        // Check if dimension changed (reset expedition)
        if (lastDimension != null && !lastDimension.equals(dimension)) {
            processExpeditionEnd(player);
        }
        lastDimension = dimension;

        // Get or create explored set for this dimension
        Set<Long> explored = exploredRegions.computeIfAbsent(dimension, k -> new HashSet<>());

        // Check if this region is new
        if (explored.contains(regionKey)) {
            // Already explored - check expedition progress
            updateExpedition(chunkPos);
            return 0;
        }

        // Check cooldown
        if (now - lastRegionTime < REGION_COOLDOWN_MS) {
            return 0;
        }

        // New region discovered!
        explored.add(regionKey);
        lastRegionTime = now;

        // Update expedition counter
        currentExpeditionChunks++;

        // Calculate points
        int points = POINTS_NEW_REGION;

        // Check for expedition bonus (every 10 new regions)
        if (currentExpeditionChunks > 0 && currentExpeditionChunks % 10 == 0) {
            points += POINTS_EXPEDITION_BONUS;
        }

        return points;
    }

    /**
     * Called when player discovers a structure.
     * Returns points earned (0 if already discovered).
     */
    public int onStructureDiscovered(ServerPlayer player, ResourceKey<Level> dimension,
                                     ResourceKey<Structure> structure, BlockPos pos) {
        Set<String> discovered = discoveredStructures.computeIfAbsent(dimension, k -> new HashSet<>());

        // Create unique structure ID (type + approximate position)
        // Use chunk-level position to avoid duplicates for same structure
        String structureId = structure.location().toString() + "_" +
                            (pos.getX() >> 9) + "_" + (pos.getZ() >> 9);

        if (discovered.contains(structureId)) {
            return 0;
        }

        discovered.add(structureId);
        return POINTS_STRUCTURE;
    }

    /**
     * Update expedition tracking when revisiting old chunk.
     */
    private void updateExpedition(ChunkPos chunkPos) {
        long newPos = ChunkPos.asLong(chunkPos.x, chunkPos.z);

        // If player is returning to previously visited area, end expedition
        if (newPos == lastChunkPos) {
            // Staying in same chunk - no change
            return;
        }

        lastChunkPos = newPos;
    }

    /**
     * Process end of expedition and award bonus if applicable.
     */
    private void processExpeditionEnd(ServerPlayer player) {
        if (currentExpeditionChunks >= 10) {
            // Award expedition bonus
            int bonusPoints = (currentExpeditionChunks / 10) * POINTS_EXPEDITION_BONUS;
            if (bonusPoints > 0) {
                LifeSkillCapability.get(player).ifPresent(manager -> {
                    int levelsGained = manager.addPoints(LifeSkillType.EXPLORATION, bonusPoints);
                    if (levelsGained > 0) {
                        Network.sendToPlayer(player, new LifeSkillLevelUpPacket(
                                LifeSkillType.EXPLORATION, manager.getLevel(LifeSkillType.EXPLORATION)));
                    }
                });
            }
        }
        currentExpeditionChunks = 0;
    }

    /**
     * Get region key from chunk position.
     * Regions are REGION_SIZE x REGION_SIZE chunks.
     */
    private long getRegionKey(ChunkPos chunkPos) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_SIZE);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_SIZE);
        return ChunkPos.asLong(regionX, regionZ);
    }

    /**
     * Get total explored region count across all dimensions.
     */
    public int getTotalExploredRegions() {
        return exploredRegions.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Get total discovered structures across all dimensions.
     */
    public int getTotalDiscoveredStructures() {
        return discoveredStructures.values().stream().mapToInt(Set::size).sum();
    }

    // === NBT Serialization ===

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Save explored regions per dimension
        CompoundTag regionsTag = new CompoundTag();
        for (Map.Entry<ResourceKey<Level>, Set<Long>> entry : exploredRegions.entrySet()) {
            long[] arr = entry.getValue().stream().mapToLong(Long::longValue).toArray();
            regionsTag.putLongArray(entry.getKey().location().toString(), arr);
        }
        tag.put("regions", regionsTag);

        // Save discovered structures per dimension
        CompoundTag structuresTag = new CompoundTag();
        for (Map.Entry<ResourceKey<Level>, Set<String>> entry : discoveredStructures.entrySet()) {
            ListTag list = new ListTag();
            for (String s : entry.getValue()) {
                list.add(net.minecraft.nbt.StringTag.valueOf(s));
            }
            structuresTag.put(entry.getKey().location().toString(), list);
        }
        tag.put("structures", structuresTag);

        // Save expedition state
        tag.putInt("expedition", currentExpeditionChunks);

        return tag;
    }

    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    public void deserializeNBT(CompoundTag tag) {
        exploredRegions.clear();
        discoveredStructures.clear();

        // Load explored regions
        CompoundTag regionsTag = tag.getCompound("regions");
        for (String dimKey : regionsTag.getAllKeys()) {
            ResourceKey<Level> dim = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(dimKey));
            long[] arr = regionsTag.getLongArray(dimKey);
            Set<Long> set = new HashSet<>();
            for (long l : arr) {
                set.add(l);
            }
            exploredRegions.put(dim, set);
        }

        // Load discovered structures
        CompoundTag structuresTag = tag.getCompound("structures");
        for (String dimKey : structuresTag.getAllKeys()) {
            ResourceKey<Level> dim = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(dimKey));
            ListTag list = structuresTag.getList(dimKey, Tag.TAG_STRING);
            Set<String> set = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                set.add(list.getString(i));
            }
            discoveredStructures.put(dim, set);
        }

        // Load expedition state
        currentExpeditionChunks = tag.getInt("expedition");
    }
}
