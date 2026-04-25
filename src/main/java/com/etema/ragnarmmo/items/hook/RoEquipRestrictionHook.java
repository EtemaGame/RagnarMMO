package com.etema.ragnarmmo.items.hook;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.items.data.RoItemRule;
import com.etema.ragnarmmo.items.runtime.RoAttributeApplier;
import com.etema.ragnarmmo.items.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.items.runtime.RoRequirementChecker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks equipping items when player doesn't meet requirements.
 * Applies attribute bonuses when requirements are met.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class RoEquipRestrictionHook {

    private RoEquipRestrictionHook() {
    }

    /**
     * Rate limiter for messages: player UUID -> last message time.
     */
    private static final Map<UUID, Long> MESSAGE_COOLDOWNS = new ConcurrentHashMap<>();

    /**
     * Cooldown between restriction messages (in milliseconds).
     */
    private static final long COOLDOWN_MS = 2000;

    /**
     * Handle equipment changes to enforce requirements and apply/remove attributes.
     * This covers all methods of equipping: active drag, hotbar swap, right-click,
     * etc.
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        EquipmentSlot slot = event.getSlot();
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        // 1. Clean up attributes from the OLD item
        RoAttributeApplier.applySlotBonuses(player, slot, RoItemRule.EMPTY, false);

        // 2. Process the NEW item
        if (to.isEmpty())
            return;

        RoItemRule rule = RoItemRuleResolver.resolve(to);

        // If no requirements, apply bonuses if any
        if (!rule.hasRequirements()) {
            RoAttributeApplier.applySlotBonuses(player, slot, rule, true);
            return;
        }

        // Check requirements
        RoRequirementChecker.CheckResult result = RoRequirementChecker.check(player, rule);

        if (result == RoRequirementChecker.CheckResult.OK ||
                result == RoRequirementChecker.CheckResult.NO_STATS_DATA) {
            // Requirements met: Apply bonuses
            RoAttributeApplier.applySlotBonuses(player, slot, rule, true);
        } else {
            // Requirements NOT met:

            // STRICT ENFORCEMENT for ARMOR: Revert equipment
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                sendRateLimitedMessage(player, result, rule);
                revertEquipment(player, slot, from, to);
            } else {
                // SOFT ENFORCEMENT for HANDS:
                // Allow holding, but DO NOT apply bonuses (already handled by not calling
                // applySlotBonuses)
                // Usability is blocked by other events (AttackEntityEvent, etc.)
            }
        }

        // Trigger sync of derived stats since equipment modifiers changed
        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            if (stats instanceof com.etema.ragnarmmo.player.stats.capability.PlayerStats internal) {
                internal.markDirty(com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.STATS);
            }
        });
    }

    private static void revertEquipment(ServerPlayer player, EquipmentSlot slot, ItemStack oldStack,
            ItemStack rejectedStack) {
        // 1. Put the old stack back in the slot
        player.setItemSlot(slot, oldStack.copy());

        // 2. Put the rejected stack back in the inventory
        if (!rejectedStack.isEmpty()) {
            if (!player.getInventory().add(rejectedStack)) {
                player.drop(rejectedStack, false);
            }
        }

        // Update container to ensure client sync
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * Block attacking with restricted items.
     */
    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return;

        RoItemRule rule = RoItemRuleResolver.resolve(stack);
        if (!rule.hasRequirements())
            return;

        RoRequirementChecker.CheckResult result = RoRequirementChecker.check(player, rule);

        if (result != RoRequirementChecker.CheckResult.OK &&
                result != RoRequirementChecker.CheckResult.NO_STATS_DATA) {

            event.setCanceled(true);
            sendRateLimitedMessage(player, result, rule);
        }
    }

    /**
     * Block usage of restricted items (Right Click).
     * This handles:
     * 1. Equipping Armor (if via right click).
     * 2. Using items (Bows, Food, Potions).
     * 3. Placing blocks (if the item is a block with requirements).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        checkAndCancel(event, event.getEntity(), event.getItemStack());
    }

    /**
     * Block using restricted items on blocks (e.g. Hoeing, Flint & Steel).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        checkAndCancel(event, event.getEntity(), event.getItemStack());
    }

    /**
     * Block breaking blocks with restricted tools.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        checkAndCancel(event, event.getEntity(), event.getItemStack());
    }

    /**
     * Common logic to check requirements and cancel interaction events.
     */
    private static void checkAndCancel(net.minecraftforge.eventbus.api.Event event,
            net.minecraft.world.entity.LivingEntity entity,
            ItemStack stack) {
        if (entity.level().isClientSide())
            return;
        if (!(entity instanceof ServerPlayer player))
            return;
        if (stack.isEmpty())
            return;

        RoItemRule rule = RoItemRuleResolver.resolve(stack);
        if (!rule.hasRequirements())
            return;

        RoRequirementChecker.CheckResult result = RoRequirementChecker.check(player, rule);

        if (result != RoRequirementChecker.CheckResult.OK
                && result != RoRequirementChecker.CheckResult.NO_STATS_DATA) {

            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            sendRateLimitedMessage(player, result, rule);
        }
    }

    /**
     * Send a rate-limited message to the player about the restriction.
     */
    private static void sendRateLimitedMessage(ServerPlayer player,
            RoRequirementChecker.CheckResult result,
            RoItemRule rule) {
        long now = System.currentTimeMillis();
        Long lastTime = MESSAGE_COOLDOWNS.get(player.getUUID());

        if (lastTime != null && (now - lastTime) < COOLDOWN_MS) {
            return; // Still on cooldown
        }

        MESSAGE_COOLDOWNS.put(player.getUUID(), now);

        Component message = switch (result) {
            case LEVEL_TOO_LOW -> Component.translatable(
                    "message.ragnarmmo.roitems.level_required",
                    rule.requiredBaseLevel());
            case WRONG_CLASS -> Component.translatable(
                    "message.ragnarmmo.roitems.class_required");
            default -> Component.translatable("message.ragnarmmo.roitems.cannot_equip");
        };

        player.displayClientMessage(message, true);
    }

    /**
     * Clean up cooldown cache on player logout to prevent memory leak.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MESSAGE_COOLDOWNS.remove(player.getUUID());
        }
    }
}
