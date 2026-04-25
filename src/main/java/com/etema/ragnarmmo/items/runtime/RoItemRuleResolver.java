package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.items.data.RoItemRule;
import com.etema.ragnarmmo.items.data.RoItemRuleLoader;
import com.etema.ragnarmmo.items.data.RoItemRuleSet;
import com.etema.ragnarmmo.items.cards.CardEquipType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.ItemStack;
import java.util.EnumMap;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves an Item to its applicable RoItemRule.
 * Uses caching for performance and implements precedence logic:
 * 1. Fallback by equipment type (lowest priority)
 * 2. Namespace + equipment type profile
 * 3. Tag match
 * 4. Exact item ID match (highest priority)
 */
public final class RoItemRuleResolver {

    // Cache: Item -> resolved RoItemRule
    // Cleared on datapack reload
    private static final Map<Item, RoItemRule> CACHE = new ConcurrentHashMap<>();

    private RoItemRuleResolver() {
    }

    /**
     * Clear the resolution cache.
     * Called when datapacks are reloaded.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Resolve the RoItemRule for an item.
     * Results are cached for performance.
     *
     * @param item the item to resolve
     * @return the resolved rule, never null (returns EMPTY if no rule applies)
     */
    public static RoItemRule resolve(Item item) {
        if (item == null) {
            return RoItemRule.EMPTY;
        }
        return CACHE.computeIfAbsent(item, RoItemRuleResolver::computeRule);
    }

    /**
     * Resolve the RoItemRule for an item stack, merging with dynamic stats.
     * 
     * @param stack the item stack
     * @return a merged rule (not cached due to NBT variance)
     */
    public static RoItemRule resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RoItemRule.EMPTY;
        }

        RoItemRule baseRule = resolve(stack.getItem());
        java.util.List<String> slottedCards = RoItemNbtHelper.getSlottedCards(stack);

        if (slottedCards.isEmpty()) {
            return baseRule;
        }

        // Merge base rule with NBT bonuses
        Map<StatKeys, Integer> mergedBonuses = new EnumMap<>(StatKeys.class);
        mergedBonuses.putAll(baseRule.attributeBonuses());

        for (String cardId : slottedCards) {
            var cardDef = com.etema.ragnarmmo.items.cards.CardRegistry.getInstance().get(cardId);
            if (cardDef != null && cardDef.modifiers() != null) {
                cardDef.modifiers().forEach((attr, val) -> {
                    if (attr.startsWith("ragnarmmo:")) {
                        try {
                            String statName = attr.substring(10).toUpperCase(java.util.Locale.ROOT);
                            StatKeys statKey = StatKeys.valueOf(statName);
                            int intVal = (int) Math.round(val);
                            mergedBonuses.put(statKey, mergedBonuses.getOrDefault(statKey, 0) + intVal);
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        }

        String baseDisplayName = baseRule.displayName() != null ? baseRule.displayName()
                : stack.getHoverName().getString();

        return new RoItemRule(
                baseDisplayName,
                mergedBonuses,
                baseRule.requiredBaseLevel(),
                baseRule.allowedJobs(),
                baseRule.cardSlots(),
                baseRule.showTooltip(),
                baseRule.combatProfile());
    }

    /**
     * Compute the rule for an item (not cached).
     * Implements precedence: fallback < mod/type < tags < itemId
     */
    private static RoItemRule computeRule(Item item) {
        RoItemRuleSet ruleSet = RoItemRuleLoader.getRuleSet();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);

        if (itemId == null) {
            return RoItemRule.EMPTY;
        }

        CardEquipType equipType = RoEquipmentTypeResolver.resolve(new ItemStack(item));
        RoItemRule resolved = RoItemRule.EMPTY;

        if (equipType != CardEquipType.ANY) {
            RoItemRule fallbackRule = ruleSet.getFallback(equipType);
            if (fallbackRule != null) {
                resolved = RoItemRule.merge(resolved, fallbackRule);
            }

            RoItemRule byModAndType = ruleSet.getByModAndType(itemId.getNamespace(), equipType);
            if (byModAndType != null) {
                resolved = RoItemRule.merge(resolved, byModAndType);
            }
        }

        for (Map.Entry<ResourceLocation, RoItemRule> entry : ruleSet.getTagRules().entrySet()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, entry.getKey());
            if (item.builtInRegistryHolder().is(tagKey)) {
                resolved = RoItemRule.merge(resolved, entry.getValue());
                break;
            }
        }

        RoItemRule byId = ruleSet.getByItemId(itemId);
        if (byId != null) {
            resolved = RoItemRule.merge(resolved, byId);
        }

        return resolved != null ? resolved : RoItemRule.EMPTY;
    }

    /**
     * Check if an item has any rule (explicit or heuristic).
     * 
     * @param item the item to check
     * @return true if a non-empty rule exists
     */
    public static boolean hasRule(Item item) {
        RoItemRule rule = resolve(item);
        return rule != null && !rule.isEmpty();
    }

    /**
     * Get cache statistics for debugging.
     * 
     * @return number of cached resolutions
     */
    public static int getCacheSize() {
        return CACHE.size();
    }
}
