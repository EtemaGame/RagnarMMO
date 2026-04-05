package com.etema.ragnarmmo.roitems.data;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.system.loot.cards.CardEquipType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for all loaded RO item rules.
 * Provides lookup by exact item ID or tag ID.
 * Thread-safe for concurrent access.
 */
public class RoItemRuleSet {

    // Rules keyed by exact item ID: "minecraft:diamond_sword"
    private final Map<ResourceLocation, RoItemRule> byItemId = new ConcurrentHashMap<>();

    // Rules keyed by tag ID (without #): "forge:swords"
    private final Map<ResourceLocation, RoItemRule> byTagId = new ConcurrentHashMap<>();

    // Rules keyed by mod namespace and equipment type.
    private final Map<String, Map<CardEquipType, RoItemRule>> byModAndType = new ConcurrentHashMap<>();

    // Generic fallback rules by equipment type.
    private final Map<CardEquipType, RoItemRule> fallbackByType = new ConcurrentHashMap<>();

    /**
     * Clear all loaded rules. Called before reload.
     */
    public void clear() {
        byItemId.clear();
        byTagId.clear();
        byModAndType.clear();
        fallbackByType.clear();
    }

    /**
     * Add a rule for a specific item ID.
     * @param itemId the item's registry name (e.g., "minecraft:diamond_sword")
     * @param rule the rule to apply
     */
    public void addItemRule(ResourceLocation itemId, RoItemRule rule) {
        if (itemId != null && rule != null) {
            byItemId.put(itemId, rule);
        }
    }

    /**
     * Add a rule for a tag.
     * @param tagId the tag ID without # prefix (e.g., "forge:swords")
     * @param rule the rule to apply to items with this tag
     */
    public void addTagRule(ResourceLocation tagId, RoItemRule rule) {
        if (tagId != null && rule != null) {
            byTagId.put(tagId, rule);
        }
    }

    public void addModTypeRule(String modId, CardEquipType equipType, RoItemRule rule) {
        if (modId == null || modId.isBlank() || equipType == null || rule == null) {
            return;
        }
        byModAndType.computeIfAbsent(modId, ignored -> new EnumMap<>(CardEquipType.class))
                .put(equipType, rule);
    }

    public void addFallbackRule(CardEquipType equipType, RoItemRule rule) {
        if (equipType != null && equipType != CardEquipType.ANY && rule != null) {
            fallbackByType.put(equipType, rule);
        }
    }

    /**
     * Get a rule by exact item ID.
     * @param itemId the item's registry name
     * @return the rule, or null if not found
     */
    public RoItemRule getByItemId(ResourceLocation itemId) {
        return byItemId.get(itemId);
    }

    /**
     * Get all item rules for syncing to clients.
     * @return unmodifiable view of item rules
     */
    public Map<ResourceLocation, RoItemRule> getItemRules() {
        return Collections.unmodifiableMap(byItemId);
    }

    /**
     * Get all tag rules for iteration during resolution.
     * @return unmodifiable view of tag rules
     */
    public Map<ResourceLocation, RoItemRule> getTagRules() {
        return Collections.unmodifiableMap(byTagId);
    }

    public RoItemRule getByModAndType(String modId, CardEquipType equipType) {
        Map<CardEquipType, RoItemRule> byType = byModAndType.get(modId);
        return byType != null ? byType.get(equipType) : null;
    }

    public RoItemRule getFallback(CardEquipType equipType) {
        return fallbackByType.get(equipType);
    }

    public Map<String, Map<CardEquipType, RoItemRule>> getModTypeRules() {
        Map<String, Map<CardEquipType, RoItemRule>> copy = new java.util.LinkedHashMap<>();
        byModAndType.forEach((modId, rules) -> {
            EnumMap<CardEquipType, RoItemRule> typedCopy = new EnumMap<>(CardEquipType.class);
            typedCopy.putAll(rules);
            copy.put(modId, Collections.unmodifiableMap(typedCopy));
        });
        return Collections.unmodifiableMap(copy);
    }

    public Map<CardEquipType, RoItemRule> getFallbackRules() {
        EnumMap<CardEquipType, RoItemRule> copy = new EnumMap<>(CardEquipType.class);
        copy.putAll(fallbackByType);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * @return number of item-specific rules loaded
     */
    public int getItemRuleCount() {
        return byItemId.size();
    }

    /**
     * @return number of tag rules loaded
     */
    public int getTagRuleCount() {
        return byTagId.size();
    }

    /**
     * @return total number of rules loaded
     */
    public int getTotalRuleCount() {
        int modTypeCount = byModAndType.values().stream().mapToInt(Map::size).sum();
        return byItemId.size() + byTagId.size() + modTypeCount + fallbackByType.size();
    }

    public int getModTypeRuleCount() {
        return byModAndType.values().stream().mapToInt(Map::size).sum();
    }

    public int getFallbackRuleCount() {
        return fallbackByType.size();
    }
}
