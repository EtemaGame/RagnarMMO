package com.etema.ragnarmmo.items.hook;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.config.access.RoItemsConfigAccess;
import com.etema.ragnarmmo.items.data.RoItemRule;
import com.etema.ragnarmmo.items.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.items.runtime.RoRequirementChecker;
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
 * Warns when player doesn't meet weapon requirements.
 * Damage penalties are applied while building the RO hand attack profile, not
 * by mutating LivingHurtEvent after combat resolution.
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
     * Emits the compatibility warning only; damage is owned by HandAttackProfileResolver.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // Only process player-caused damage
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        // Check if system is enabled
        if (!RoItemsConfigAccess.isEnabled() || !RoItemsConfigAccess.reduceDamageOnRestriction())
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

        long cooldownMs = RoItemsConfigAccess.getMessageCooldownMs();
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
        long cooldownMs = RoItemsConfigAccess.getMessageCooldownMs();
        COMBAT_MSG_COOLDOWNS.entrySet().removeIf(entry -> (now - entry.getValue()) > cooldownMs * 10);
    }
}
