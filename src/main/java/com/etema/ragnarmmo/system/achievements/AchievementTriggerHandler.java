package com.etema.ragnarmmo.system.achievements;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.system.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.system.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.system.achievements.data.AchievementRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles listening to gameplay events and progressing achievements.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AchievementTriggerHandler {

    /**
     * Called whenever a player levels up their Base Level or Job Level.
     */
    public static void onPlayerLevelUp(ServerPlayer player, int newBaseLevel, int newJobLevel) {
        checkTriggers(player, "level_up", "base", newBaseLevel);
        checkTriggers(player, "level_up", "job", newJobLevel);
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (typeId != null) {
                checkTriggers(player, "kill_mob", typeId.toString(), 1);
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getItem().getItem().getItem());
            if (itemId != null) {
                int count = event.getItem().getItem().getCount();
                checkTriggers(player, "pickup_item", itemId.toString(), count);
            }
        }
    }

    /**
     * Checks all achievements to see if the given trigger event advances them.
     * 
     * @param player      The player to check
     * @param triggerType The type of event (e.g. "kill_mob", "level_up")
     * @param targetId    The specific target (e.g. "minecraft:zombie", "base")
     * @param amount      The amount to add (e.g. 1 kill, or the new level number)
     */
    private static void checkTriggers(ServerPlayer player, String triggerType, String targetId, int amount) {
        player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            boolean didUpdate = false;

            for (AchievementDefinition def : AchievementRegistry.getInstance().getAll().values()) {
                // Skip already unlocked ones
                if (cap.isUnlocked(def.id())) {
                    continue;
                }

                // Match trigger type
                if (!def.triggerType().equals(triggerType)) {
                    continue;
                }

                // Match specific target if the definition requires one
                if (def.triggerId() != null && !def.triggerId().equals(targetId)) {
                    continue;
                }

                // Special handling for Level Up where 'amount' is the current static value
                if (triggerType.equals("level_up")) {
                    if (amount >= def.requiredAmount()) {
                        cap.unlockAchievement(def.id(), def.points());
                        notifyPlayerUnlocked(player, def);
                        didUpdate = true;
                    }
                }
                // Incremental progress (e.g. collecting 100 apples or killing 50 zombies)
                else {
                    String trackerKey = def.id() + "_progress";
                    cap.addProgress(trackerKey, amount);

                    if (cap.getProgress(trackerKey) >= def.requiredAmount()) {
                        cap.unlockAchievement(def.id(), def.points());
                        notifyPlayerUnlocked(player, def);
                        didUpdate = true;
                    }
                }
            }

            if (didUpdate) {
                // TODO: Sync capability packet
            }
        });
    }

    private static void notifyPlayerUnlocked(ServerPlayer player, AchievementDefinition def) {
        // Send a localized chat message or toast informing the player
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component
                .translatable("achievement.ragnarmmo.unlocked", def.name())
                .withStyle(net.minecraft.ChatFormatting.YELLOW);
        player.sendSystemMessage(msg);
    }
}
