package com.etema.ragnarmmo.system.loot.cards;

import java.util.Map;

/**
 * Data definition for a card loaded from JSON data packs.
 *
 * @param id          unique card identifier (e.g. "zombie_card")
 * @param displayName human-readable name (e.g. "Zombie Card")
 * @param mobId       registry key of the mob that drops this card (e.g.
 *                    "minecraft:zombie")
 * @param modifiers   attribute modifiers granted by the card (e.g.
 *                    {"ragnarmmo:vit": 1.0})
 * @param dropRate    base drop chance (0.0–1.0) before LUK scaling
 * @param rarity      rarity tier (e.g. "UNCOMMON", "RARE", "EPIC", "LEGENDARY")
 */
public record CardDefinition(
        String id,
        String displayName,
        String mobId,
        Map<String, Double> modifiers,
        double dropRate,
        String translationKey,
        int modelId) {
}
