package com.etema.ragnarmmo.roitems.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
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

    /**
     * Clear all loaded rules. Called before reload.
     */
    public void clear() {
        byItemId.clear();
        byTagId.clear();
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
        return byItemId.size() + byTagId.size();
    }
}
