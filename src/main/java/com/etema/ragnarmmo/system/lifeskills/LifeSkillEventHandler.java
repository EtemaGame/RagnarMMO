package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Life Skill point gains from block breaking.
 * Integrates with PlacedBlockTracker to prevent exploits.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class LifeSkillEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifeSkillEventHandler.class);

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
            return;
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        // Check if block was player-placed (anti-exploit)
        PlacedBlockTracker tracker = PlacedBlockTracker.get(serverLevel);
        if (tracker.isPlayerPlaced(pos)) {
            // Player-placed blocks don't give points
            LOGGER.debug("Skipping placed block at {}", pos);
            return;
        }

        // Get point source configuration
        LifePointsSourceConfig config = LifePointsSourceConfig.getInstance();
        if (config == null)
            return;

        LifePointsSourceConfig.PointSource source = config.getSource(state);
        if (source == null) {
            // Only log unique/rare blocks to avoid spam, or strictly for debugging session
            LOGGER.debug("No source for block {} at {}", state.getBlock(), pos);
            return;
        }

        LifeSkillType skill = source.getSkill();
        if (skill == null)
            return;

        // Process point gain
        LifeSkillCapability.get(player).ifPresent(manager -> {
            LifeSkillProgress progress = manager.getSkill(skill);
            if (progress == null)
                return;

            int levelsGained;
            int pointsAwarded;
            String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();

            if (source.usesThreshold()) {
                // Accumulate counter, award points when threshold reached
                pointsAwarded = progress.incrementBlockCounter(
                        blockId,
                        source.getThreshold(),
                        source.getPoints());
                if (pointsAwarded > 0) {
                    levelsGained = progress.addPoints(pointsAwarded);
                } else {
                    levelsGained = 0;
                }
            } else {
                // Direct points per block
                pointsAwarded = source.getPoints();
                levelsGained = progress.addPoints(pointsAwarded);
            }

            // Send updates to client
            if (pointsAwarded > 0) {
                // Send point gain notification
                Network.sendToPlayer(player, new LifeSkillPointsPacket(
                        skill, pointsAwarded, progress.getLevel(), progress.getPoints()));

                // Send level up notification if applicable
                if (levelsGained > 0) {
                    Network.sendToPlayer(player, new LifeSkillLevelUpPacket(
                            skill, progress.getLevel()));

                    // Check for pending perk choice
                    if (progress.hasPendingPerkChoice()) {
                        Network.sendToPlayer(player, new LifeSkillPerkChoicePacket(
                                skill, progress.getPendingPerkTier()));
                    }
                }
            }
        });
    }
}
