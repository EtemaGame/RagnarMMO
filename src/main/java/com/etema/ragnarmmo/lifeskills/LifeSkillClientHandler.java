package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Collections;

/**
 * Client-side handler for life skill notifications.
 * Manages buffering and throttling of point gain messages.
 */
@OnlyIn(Dist.CLIENT)
public class LifeSkillClientHandler {

    // Buffer for point gains to prevent spam
    private static final Deque<PointGainEntry> pointGainBuffer = new ArrayDeque<>();
    private static long lastPointDisplay = 0;
    private static final long POINT_DISPLAY_COOLDOWN = 1500; // 1.5 seconds

    // Level up display
    private static LifeSkillType levelUpSkill = null;
    private static int levelUpLevel = 0;
    private static long levelUpTime = 0;
    private static final long LEVEL_UP_DURATION = 5000; // 5 seconds

    // Perk choice display
    private static LifeSkillType perkChoiceSkill = null;
    private static int perkChoiceTier = 0;
    private static boolean showPerkChoiceScreen = false;

    // Accumulated points for display (throttling)
    private static final Map<LifeSkillType, Integer> accumulatedPoints = new EnumMap<>(LifeSkillType.class);
    private static long lastAccumulationTime = 0;

    /**
     * Called when points are gained.
     * Buffers points and displays accumulated total after cooldown.
     */
    public static void showPointsGain(LifeSkillType skill, int points, int currentLevel, int currentPoints) {
        long now = System.currentTimeMillis();

        // Accumulate points
        accumulatedPoints.merge(skill, points, Integer::sum);
        lastAccumulationTime = now;

        // Update local capability data
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            LifeSkillCapability.get(mc.player).ifPresent(manager -> {
                LifeSkillProgress progress = manager.getSkill(skill);
                if (progress != null) {
                    progress.setLevelAndPoints(currentLevel, currentPoints);
                }
            });
        }
    }

    /**
     * Called when a level up occurs.
     */
    public static void showLevelUp(LifeSkillType skill, int newLevel) {
        levelUpSkill = skill;
        levelUpLevel = newLevel;
        levelUpTime = System.currentTimeMillis();

        // Update local capability data
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            LifeSkillCapability.get(mc.player).ifPresent(manager -> {
                LifeSkillProgress progress = manager.getSkill(skill);
                if (progress != null) {
                    progress.setLevelClamped(newLevel);
                }
            });
        }
    }

    /**
     * Called when a perk choice is available.
     */
    public static void showPerkChoice(LifeSkillType skill, int tier) {
        perkChoiceSkill = skill;
        perkChoiceTier = tier;
        showPerkChoiceScreen = true;
    }

    /**
     * Get accumulated points for display, then clear.
     * Called by overlay renderer.
     */
    public static Map<LifeSkillType, Integer> consumeAccumulatedPoints() {
        long now = System.currentTimeMillis();
        if (now - lastAccumulationTime < POINT_DISPLAY_COOLDOWN) {
            return Collections.emptyMap();
        }

        if (accumulatedPoints.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<LifeSkillType, Integer> result = new EnumMap<>(accumulatedPoints);
        accumulatedPoints.clear();
        lastPointDisplay = now;
        return result;
    }

    /**
     * Check if there's a level up to display.
     */
    public static boolean hasLevelUpToShow() {
        if (levelUpSkill == null)
            return false;
        return System.currentTimeMillis() - levelUpTime < LEVEL_UP_DURATION;
    }

    public static LifeSkillType getLevelUpSkill() {
        return levelUpSkill;
    }

    public static int getLevelUpLevel() {
        return levelUpLevel;
    }

    public static float getLevelUpAlpha() {
        long elapsed = System.currentTimeMillis() - levelUpTime;
        if (elapsed > LEVEL_UP_DURATION - 1000) {
            // Fade out in last second
            return 1.0f - (elapsed - (LEVEL_UP_DURATION - 1000)) / 1000.0f;
        }
        return 1.0f;
    }

    /**
     * Check if perk choice screen should be shown.
     */
    public static boolean shouldShowPerkChoice() {
        return showPerkChoiceScreen && perkChoiceSkill != null;
    }

    public static LifeSkillType getPerkChoiceSkill() {
        return perkChoiceSkill;
    }

    public static int getPerkChoiceTier() {
        return perkChoiceTier;
    }

    public static void closePerkChoice() {
        showPerkChoiceScreen = false;
        perkChoiceSkill = null;
        perkChoiceTier = 0;
    }

    /**
     * Get skill icon character for HUD display.
     */
    public static String getSkillIcon(LifeSkillType skill) {
        if (skill == null)
            return "\u2B50"; // Star
        return switch (skill) {
            case MINING -> "\u26CF"; // Pick
            case WOODCUTTING -> "\uD83E\uDE93"; // Axe (or tree)
            case EXCAVATION -> "\uD83E\uDEA3"; // Shovel-like
            case FARMING -> "\uD83C\uDF3E"; // Wheat
            case FISHING -> "\uD83C\uDFA3"; // Fishing pole
            case EXPLORATION -> "\uD83E\uDDED"; // Compass
        };
    }

    // Entry for buffered point gains
    private static class PointGainEntry {
        final LifeSkillType skill;
        final int points;
        final long time;

        PointGainEntry(LifeSkillType skill, int points) {
            this.skill = skill;
            this.points = points;
            this.time = System.currentTimeMillis();
        }
    }
}
