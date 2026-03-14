package com.etema.ragnarmmo.roitems.hooks.server;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.config.RoItemsConfig;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoAttributeApplier;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.roitems.runtime.RoRequirementChecker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors mainhand/offhand changes via tick polling.
 * Handles hotbar scroll which does NOT fire LivingEquipmentChangeEvent.
 *
 * This hook ensures that weapon attribute bonuses are correctly applied/removed
 * when the player scrolls their hotbar to change their held item.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class RoHotbarTickHook {

    private RoHotbarTickHook() {
    }

    /**
     * Cache of last known hand items per player.
     * Key: Player UUID
     * Value: HandCache containing last mainhand and offhand items
     */
    private static final Map<UUID, HandCache> PLAYER_HAND_CACHE = new ConcurrentHashMap<>();

    /**
     * Tick counter per player to implement configurable check interval.
     */
    private static final Map<UUID, Integer> TICK_COUNTERS = new ConcurrentHashMap<>();

    /**
     * Immutable record to store last known hand items.
     * Uses Item reference for efficient comparison (we only care about item TYPE,
     * not NBT).
     */
    private record HandCache(ItemStack mainhandItem, ItemStack offhandItem) {

        static HandCache from(ServerPlayer player) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            return new HandCache(
                    main.copy(),
                    off.copy());
        }

        boolean mainhandChanged(HandCache other) {
            if (other == null)
                return true;
            return !ItemStack.isSameItemSameTags(this.mainhandItem, other.mainhandItem);
        }

        boolean offhandChanged(HandCache other) {
            if (other == null)
                return true;
            return !ItemStack.isSameItemSameTags(this.offhandItem, other.offhandItem);
        }
    }

    /**
     * Poll for hand item changes every N ticks (from config).
     * Only runs server-side, Phase.END to avoid conflicts.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only server side, end phase
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.player.level().isClientSide())
            return;
        if (!(event.player instanceof ServerPlayer player))
            return;

        // Check if system is enabled
        if (!RoItemsConfig.isEnabled())
            return;

        UUID playerId = player.getUUID();

        // Tick throttling
        int tickCounter = TICK_COUNTERS.getOrDefault(playerId, 0) + 1;
        int checkInterval = RoItemsConfig.getTickCheckInterval();

        if (tickCounter < checkInterval) {
            TICK_COUNTERS.put(playerId, tickCounter);
            return;
        }

        // Reset counter
        TICK_COUNTERS.put(playerId, 0);

        // Get current and cached hand states
        HandCache current = HandCache.from(player);
        HandCache cached = PLAYER_HAND_CACHE.get(playerId);

        // Check for changes
        boolean mainhandChanged = current.mainhandChanged(cached);
        boolean offhandChanged = current.offhandChanged(cached);

        // Update cache regardless
        PLAYER_HAND_CACHE.put(playerId, current);

        // Process changes
        if (mainhandChanged) {
            processSlotChange(player, EquipmentSlot.MAINHAND);
        }
        if (offhandChanged) {
            processSlotChange(player, EquipmentSlot.OFFHAND);
        }
    }

    /**
     * Process a slot change: resolve rule, check requirements, apply/remove
     * bonuses.
     * NOTE: We do NOT send messages here to avoid spam on every hotbar scroll.
     * Combat restriction hook handles messaging when player attacks.
     */
    private static void processSlotChange(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);

        if (stack.isEmpty()) {
            // Slot is now empty - clear any bonuses
            RoAttributeApplier.applySlotBonuses(player, slot, RoItemRule.EMPTY, false);
            return;
        }

        RoItemRule rule = RoItemRuleResolver.resolve(stack);

        if (!rule.hasRequirements()) {
            // No requirements - apply bonuses unconditionally
            RoAttributeApplier.applySlotBonuses(player, slot, rule, true);
            return;
        }

        // Check requirements
        RoRequirementChecker.CheckResult result = RoRequirementChecker.check(player, rule);
        boolean meetsReq = (result == RoRequirementChecker.CheckResult.OK
                || result == RoRequirementChecker.CheckResult.NO_STATS_DATA);

        RoAttributeApplier.applySlotBonuses(player, slot, rule, meetsReq);
    }

    /**
     * Clean up cache on player logout.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            PLAYER_HAND_CACHE.remove(id);
            TICK_COUNTERS.remove(id);
        }
    }

    /**
     * Initialize cache on player login to avoid false-positive change detection.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HandCache initial = HandCache.from(player);
            PLAYER_HAND_CACHE.put(player.getUUID(), initial);
            TICK_COUNTERS.put(player.getUUID(), 0);
        }
    }
}
