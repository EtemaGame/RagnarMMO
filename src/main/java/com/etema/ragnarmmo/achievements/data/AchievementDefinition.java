package com.etema.ragnarmmo.achievements.data;

import org.jetbrains.annotations.Nullable;
import java.util.Map;

/**
 * Represents a data-driven achievement loaded from JSON.
 * 
 * @param id             Unique identifier (e.g. "reach_level_10")
 * @param category       The tab/category this achievement belongs to
 * @param name           Translation key or raw string for achievement name
 * @param description    Translation key or raw string for description
 * @param triggerType    The type of game event that progresses this (e.g.
 *                       "level_up", "kill_mob")
 * @param triggerId      Specific target ID for the trigger (e.g.
 *                       "minecraft:zombie") if applicable
 * @param requiredAmount The amount needed to complete (e.g. 50 kills, Level 10)
 * @param points         The number of achievement points awarded
 * @param title          The custom title granted (e.g. "Novice Adapter"), or
 *                       null
 * @param rewards        Item IDs and counts to award upon claiming
 */
public record AchievementDefinition(
        String id,
        AchievementCategory category,
        String name,
        String description,
        String triggerType,
        @Nullable String triggerId,
        int requiredAmount,
        int points,
        @Nullable String title,
        Map<String, Integer> rewards) {
}
