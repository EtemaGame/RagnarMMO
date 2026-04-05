package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles Fishing life skill point gains.
 * Points are awarded based on what is caught.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class FishingEventHandler {

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        LifeSkillCapability.get(player).ifPresent(manager -> {
            LifeSkillProgress progress = manager.getSkill(LifeSkillType.FISHING);
            if (progress == null)
                return;

            int totalPoints = 0;
            int levelsGained = 0;

            // Process each caught item
            for (ItemStack stack : event.getDrops()) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemId == null)
                    continue;

                // Get fishing points from config
                LifePointsSourceConfig.PointSource source = LifePointsSourceConfig.getInstance().getSource(itemId);
                if (source == null || source.getSkill() != LifeSkillType.FISHING) {
                    // Default points for unlisted catches if they don't have a specific config
                    // We'll give a generic 2 points per 10 items
                    int pointsAwarded = progress.incrementBlockCounter(
                            itemId.toString(),
                            10,
                            2);
                    if (pointsAwarded > 0) {
                        levelsGained += manager.addPoints(LifeSkillType.FISHING, pointsAwarded);
                        totalPoints += pointsAwarded;
                    }
                    continue;
                }

                int pointsAwarded;
                if (source.usesThreshold()) {
                    // Accumulate counter
                    pointsAwarded = progress.incrementBlockCounter(
                            itemId.toString(),
                            source.getThreshold(),
                            source.getPoints());
                    if (pointsAwarded > 0) {
                        levelsGained += manager.addPoints(LifeSkillType.FISHING, pointsAwarded);
                        totalPoints += pointsAwarded;
                    }
                } else {
                    // Direct points per catch
                    pointsAwarded = source.getPoints() * stack.getCount();
                    levelsGained += manager.addPoints(LifeSkillType.FISHING, pointsAwarded);
                    totalPoints += pointsAwarded;
                }
            }

            // Send updates to client
            if (totalPoints > 0) {
                Network.sendToPlayer(player, new LifeSkillPointsPacket(
                        LifeSkillType.FISHING, totalPoints, progress.getLevel(), progress.getPoints()));

                if (levelsGained > 0) {
                    Network.sendToPlayer(player, new LifeSkillLevelUpPacket(
                            LifeSkillType.FISHING, progress.getLevel()));

                    if (progress.hasPendingPerkChoice()) {
                        Network.sendToPlayer(player, new LifeSkillPerkChoicePacket(
                                LifeSkillType.FISHING, progress.getPendingPerkTier()));
                    }
                }
            }
        });
    }

    // getFishingSource and FishingPointSource have been migrated to data-driven
    // LifePointsSourceConfig
}
