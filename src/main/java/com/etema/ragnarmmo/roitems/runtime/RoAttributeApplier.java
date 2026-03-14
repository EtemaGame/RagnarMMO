package com.etema.ragnarmmo.roitems.runtime;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.util.AttrUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies and removes transient attribute modifiers to players based on
 * equipped items.
 * Uses deterministic UUIDs per (slot, stat) combination to prevent duplicates.
 */
public final class RoAttributeApplier {

    private RoAttributeApplier() {
    }

    /**
     * All equipment slots tracked by the RO items system.
     */
    public static final EquipmentSlot[] TRACKED_SLOTS = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /**
     * Mapping from StatKeys to RagnarAttributes registry objects.
     */
    private static final Map<StatKeys, RegistryObject<Attribute>> STAT_TO_ATTR = new EnumMap<>(StatKeys.class);

    static {
        STAT_TO_ATTR.put(StatKeys.STR, RagnarAttributes.STR);
        STAT_TO_ATTR.put(StatKeys.AGI, RagnarAttributes.AGI);
        STAT_TO_ATTR.put(StatKeys.VIT, RagnarAttributes.VIT);
        STAT_TO_ATTR.put(StatKeys.INT, RagnarAttributes.INT);
        STAT_TO_ATTR.put(StatKeys.DEX, RagnarAttributes.DEX);
        STAT_TO_ATTR.put(StatKeys.LUK, RagnarAttributes.LUK);
    }

    /**
     * Cache for modifier UUIDs to avoid recomputing.
     */
    private static final Map<String, UUID> UUID_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Generate a stable, deterministic UUID for a (slot, stat) combination.
     * This ensures the same modifier is always identified by the same UUID,
     * preventing duplicates across reconnects or dimension changes.
     *
     * @param slot the equipment slot
     * @param stat the stat key
     * @return a deterministic UUID
     */
    public static UUID getModifierUUID(EquipmentSlot slot, StatKeys stat) {
        String key = "roitems:" + slot.name() + "_" + stat.name();
        return UUID_CACHE.computeIfAbsent(key, k -> UUID.nameUUIDFromBytes(k.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Generate the modifier name for logging/debugging.
     *
     * @param slot the equipment slot
     * @param stat the stat key
     * @return the modifier name
     */
    public static String getModifierName(EquipmentSlot slot, StatKeys stat) {
        return "RoItem_" + slot.name() + "_" + stat.name();
    }

    /**
     * Apply or remove attribute bonuses for a single equipment slot.
     * If meetsRequirements is false or rule is empty, bonuses are cleared.
     *
     * @param player            the player to modify
     * @param slot              the equipment slot
     * @param rule              the item rule (may be null or EMPTY)
     * @param meetsRequirements whether the player meets the item's requirements
     */
    public static void applySlotBonuses(Player player, EquipmentSlot slot,
            RoItemRule rule, boolean meetsRequirements) {
        if (player == null)
            return;

        for (StatKeys stat : StatKeys.values()) {
            UUID uuid = getModifierUUID(slot, stat);
            RegistryObject<Attribute> attrObj = STAT_TO_ATTR.get(stat);
            if (attrObj == null || !attrObj.isPresent())
                continue;

            AttributeInstance instance = player.getAttribute(attrObj.get());
            if (instance == null)
                continue;

            // Determine the bonus amount
            double amount = 0.0;
            if (meetsRequirements && rule != null && rule.hasAttributeBonuses()) {
                amount = rule.getBonus(stat);
            }

            // Use the existing AttrUtil pattern from the codebase
            AttrUtil.upsertTransient(
                    instance,
                    uuid,
                    getModifierName(slot, stat),
                    amount,
                    AttributeModifier.Operation.ADDITION);
        }
    }

    /**
     * Clear all RO item bonuses from a player.
     * Called on logout, death, or when needed.
     *
     * @param player the player to clear bonuses from
     */
    public static void clearAllBonuses(Player player) {
        if (player == null)
            return;

        for (EquipmentSlot slot : TRACKED_SLOTS) {
            applySlotBonuses(player, slot, RoItemRule.EMPTY, false);
        }
    }

    /**
     * Refresh all slot bonuses for a player.
     * Resolves rules for all equipped items and applies bonuses.
     *
     * @param player the player to refresh
     */
    public static void refreshAllSlots(Player player) {
        if (player == null)
            return;

        for (EquipmentSlot slot : TRACKED_SLOTS) {
            var stack = player.getItemBySlot(slot);
            RoItemRule rule = RoItemRule.EMPTY;
            boolean meetsReq = true;

            if (!stack.isEmpty()) {
                rule = RoItemRuleResolver.resolve(stack);
                meetsReq = RoRequirementChecker.meetsRequirements(player, rule);
            }

            applySlotBonuses(player, slot, rule, meetsReq);
        }
    }

    /**
     * Get the total attribute bonus a player is receiving from RO items.
     * Useful for UI/debugging.
     *
     * @param player the player
     * @param stat   the stat to check
     * @return total bonus from all slots
     */
    public static int getTotalBonus(Player player, StatKeys stat) {
        if (player == null)
            return 0;

        int total = 0;
        for (EquipmentSlot slot : TRACKED_SLOTS) {
            var stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                RoItemRule rule = RoItemRuleResolver.resolve(stack);
                if (RoRequirementChecker.meetsRequirements(player, rule)) {
                    total += rule.getBonus(stat);
                }
            }
        }
        return total;
    }
}
