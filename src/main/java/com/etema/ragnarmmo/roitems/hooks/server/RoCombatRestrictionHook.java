package com.etema.ragnarmmo.roitems.hooks.server;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.config.RoItemsConfig;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.roitems.runtime.RoRequirementChecker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reduces weapon damage when player doesn't meet weapon requirements.
 * Acts as a "last line of defense" if a restricted weapon is somehow equipped.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class RoCombatRestrictionHook {

    private RoCombatRestrictionHook() {
    }

    /**
     * Rate limiter for combat warning messages.
     */
    private static final Map<UUID, Long> COMBAT_MSG_COOLDOWNS = new ConcurrentHashMap<>();

    /**
     * Reduce damage to configured penalty if weapon requirements are not met.
     * Uses HIGH priority to run before main damage calculations.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // Only process player-caused damage
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        // Check if system is enabled
        if (!RoItemsConfig.isEnabled() || !RoItemsConfig.shouldReduceDamage())
            return;

        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (weapon.isEmpty())
            return;

        RoItemRule rule = RoItemRuleResolver.resolve(weapon);

        // Skip if no requirements
        if (!rule.hasRequirements())
            return;

        RoRequirementChecker.CheckResult result = RoRequirementChecker.check(player, rule);

        // If requirements not met (and not missing stats data), apply penalty
        if (result != RoRequirementChecker.CheckResult.OK
                && result != RoRequirementChecker.CheckResult.NO_STATS_DATA) {

            // Reduce damage to configured penalty (default: 0)
            event.setAmount((float) RoItemsConfig.getPenaltyDamage());

            // Send rate-limited warning with specific reason
            sendCombatWarning(player, result, rule);
        }
    }

    /**
     * Send a rate-limited warning message about ineffective weapon.
     * Shows specific reason (level or class) like RoEquipRestrictionHook.
     */
    private static void sendCombatWarning(ServerPlayer player,
            RoRequirementChecker.CheckResult result,
            RoItemRule rule) {
        long now = System.currentTimeMillis();
        Long lastTime = COMBAT_MSG_COOLDOWNS.get(player.getUUID());

        long cooldownMs = RoItemsConfig.getMessageCooldownMs();
        if (lastTime != null && (now - lastTime) < cooldownMs) {
            return; // Still on cooldown
        }

        COMBAT_MSG_COOLDOWNS.put(player.getUUID(), now);

        // Match RoEquipRestrictionHook message style
        Component message = switch (result) {
            case LEVEL_TOO_LOW -> Component.translatable(
                    "message.ragnarmmo.roitems.level_required",
                    rule.requiredBaseLevel());
            case WRONG_CLASS -> Component.translatable(
                    "message.ragnarmmo.roitems.class_required");
            default -> Component.translatable("message.ragnarmmo.roitems.weapon_ineffective");
        };

        player.displayClientMessage(message, true); // Action bar
    }

    /**
     * Clean up cooldown entries periodically.
     */
    public static void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        long cooldownMs = RoItemsConfig.getMessageCooldownMs();
        COMBAT_MSG_COOLDOWNS.entrySet().removeIf(entry -> (now - entry.getValue()) > cooldownMs * 10);
    }
}
