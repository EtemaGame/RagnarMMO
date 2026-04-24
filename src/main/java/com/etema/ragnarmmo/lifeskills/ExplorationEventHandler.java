package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles Exploration skill point gains from chunk discovery.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class ExplorationEventHandler {

    // Track last chunk per player to detect chunk changes
    private static final java.util.Map<java.util.UUID, Long> lastPlayerChunk = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Only check every 20 ticks (1 second) for performance
        if (player.tickCount % 20 != 0) return;

        ChunkPos currentChunk = new ChunkPos(player.blockPosition());
        long currentChunkKey = currentChunk.toLong();

        Long lastChunk = lastPlayerChunk.get(player.getUUID());
        if (lastChunk != null && lastChunk == currentChunkKey) {
            return; // Same chunk, no change
        }

        lastPlayerChunk.put(player.getUUID(), currentChunkKey);

        // Process chunk entry for exploration
        LifeSkillCapability.get(player).ifPresent(manager -> {
            LifeSkillProgress progress = manager.getSkill(LifeSkillType.EXPLORATION);
            if (progress == null) return;

            // Get or create exploration tracker from progress (stored in a field we need to add)
            // For now, use a simple region-based approach
            int points = calculateExplorationPoints(player, currentChunk);
            if (points > 0) {
                int levelsGained = manager.addPoints(LifeSkillType.EXPLORATION, points);

                // Send update to client
                Network.sendToPlayer(player, new LifeSkillPointsPacket(
                        LifeSkillType.EXPLORATION, points, progress.getLevel(), progress.getPoints()));

                if (levelsGained > 0) {
                    Network.sendToPlayer(player, new LifeSkillLevelUpPacket(
                            LifeSkillType.EXPLORATION, progress.getLevel()));

                    if (progress.hasPendingPerkChoice()) {
                        Network.sendToPlayer(player, new LifeSkillPerkChoicePacket(
                                LifeSkillType.EXPLORATION, progress.getPendingPerkTier()));
                    }
                }
            }
        });
    }

    /**
     * Calculate exploration points for entering a chunk.
     * Uses region-based tracking stored in player data.
     */
    private static int calculateExplorationPoints(ServerPlayer player, ChunkPos chunk) {
        // Region = 4x4 chunks, persisted inside the life skill progress itself.
        int regionX = Math.floorDiv(chunk.x, 4);
        int regionZ = Math.floorDiv(chunk.z, 4);
        String regionKey = player.level().dimension().location() + "_" + regionX + "_" + regionZ;

        return LifeSkillCapability.get(player).map(manager -> {
            LifeSkillProgress progress = manager.getSkill(LifeSkillType.EXPLORATION);
            if (progress == null) {
                return 0;
            }

            if (progress.markUniqueDiscovery(regionKey)) {
                return 2; // First time in this region
            }

            return 0;
        }).orElse(0);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up tracking map
        lastPlayerChunk.remove(event.getEntity().getUUID());
    }
}
