package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks point-based progression for a single Life Skill.
 * Uses the formula: requiredPoints(level) = round(100 * 1.5^(level-1))
 */
public class LifeSkillProgress {

    public static final int MAX_LEVEL = 100;
    public static final double BASE_POINTS = 100.0;
    public static final double GROWTH_RATE = 1.5;
    public static final int PERK_INTERVAL = 10; // Every 10 levels

    private final LifeSkillType skillType;
    private int level;
    private int points;
    private int totalPointsEarned;
    private final List<String> chosenPerks; // Perk IDs chosen by player

    // Counters for "per X blocks" accumulation
    private final java.util.Map<String, Integer> blockCounters;

    public LifeSkillProgress(LifeSkillType skillType) {
        this.skillType = skillType;
        this.level = 1;
        this.points = 0;
        this.totalPointsEarned = 0;
        this.chosenPerks = new ArrayList<>();
        this.blockCounters = new java.util.HashMap<>();
    }

    public LifeSkillType getSkillType() {
        return skillType;
    }

    public int getLevel() {
        return level;
    }

    public int getPoints() {
        return points;
    }

    public int getTotalPointsEarned() {
        return totalPointsEarned;
    }

    public List<String> getChosenPerks() {
        return new ArrayList<>(chosenPerks);
    }

    /**
     * Calculate points required to advance from current level to next.
     * Formula: round(100 * 1.5^(level-1))
     */
    public static int getRequiredPoints(int level) {
        if (level <= 0)
            return 0;
        if (level >= MAX_LEVEL)
            return Integer.MAX_VALUE;
        return (int) Math.round(BASE_POINTS * Math.pow(GROWTH_RATE, level - 1));
    }

    public int getPointsToNextLevel() {
        return getRequiredPoints(level);
    }

    public double getProgressPercent() {
        if (level >= MAX_LEVEL)
            return 1.0;
        int required = getPointsToNextLevel();
        return required > 0 ? Math.min(1.0, (double) points / required) : 0.0;
    }

    /**
     * Add points and process level ups.
     * 
     * @param amount Points to add
     * @return Number of levels gained
     */
    public int addPoints(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL)
            return 0;

        this.points += amount;
        this.totalPointsEarned += amount;

        int levelsGained = 0;
        int required = getPointsToNextLevel();

        while (points >= required && level < MAX_LEVEL) {
            points -= required;
            level++;
            levelsGained++;
            required = getPointsToNextLevel();
        }

        // Cap points at max level
        if (level >= MAX_LEVEL) {
            points = 0;
        }

        return levelsGained;
    }

    /**
     * Increment block counter and return points to award.
     * Used for "per X blocks" point sources.
     */
    public int incrementBlockCounter(String blockId, int threshold, int pointsPerThreshold) {
        int current = blockCounters.getOrDefault(blockId, 0) + 1;
        int pointsEarned = 0;

        while (current >= threshold) {
            current -= threshold;
            pointsEarned += pointsPerThreshold;
        }

        blockCounters.put(blockId, current);
        return pointsEarned;
    }

    /**
     * Mark a persistent discovery flag.
     *
     * <p>Returns true only the first time a key is seen. Used for exploration
     * regions and other one-time discoveries that must persist across sessions.</p>
     */
    public boolean markUniqueDiscovery(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (blockCounters.containsKey(key)) {
            return false;
        }
        blockCounters.put(key, 1);
        return true;
    }

    /**
     * Check if player has a pending perk choice.
     * A perk choice is pending every PERK_INTERVAL levels if not chosen.
     */
    public boolean hasPendingPerkChoice() {
        int expectedPerks = level / PERK_INTERVAL;
        return chosenPerks.size() < expectedPerks;
    }

    /**
     * Get the tier of the pending perk choice (1, 2, 3, etc.)
     */
    public int getPendingPerkTier() {
        return chosenPerks.size() + 1;
    }

    /**
     * Add a chosen perk.
     */
    public void addPerk(String perkId) {
        if (!chosenPerks.contains(perkId)) {
            chosenPerks.add(perkId);
        }
    }

    /**
     * Check if player has a specific perk.
     */
    public boolean hasPerk(String perkId) {
        return chosenPerks.contains(perkId);
    }

    /**
     * Set level directly (for commands/loading).
     */
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(level, MAX_LEVEL));
        this.points = 0;
    }

    public void setLevelClamped(int level) {
        this.level = Math.max(1, Math.min(level, MAX_LEVEL));
        if (this.level >= MAX_LEVEL) {
            this.points = 0;
        }
    }

    public void setPointsClamped(int points) {
        if (this.level >= MAX_LEVEL) {
            this.points = 0;
            return;
        }
        int required = getPointsToNextLevel();
        // clamp a [0, required-1] para evitar estados inválidos
        this.points = Math.max(0, Math.min(points, Math.max(0, required - 1)));
    }

    public void setLevelAndPoints(int level, int points) {
        setLevelClamped(level);
        setPointsClamped(points);
    }

    // === NBT Serialization ===

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("points", points);
        tag.putInt("totalPoints", totalPointsEarned);

        // Save perks
        ListTag perkList = new ListTag();
        for (String perk : chosenPerks) {
            perkList.add(StringTag.valueOf(perk));
        }
        tag.put("perks", perkList);

        // Save counters
        CompoundTag countersTag = new CompoundTag();
        for (var entry : blockCounters.entrySet()) {
            countersTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("counters", countersTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.level = tag.getInt("level");
        if (this.level < 1)
            this.level = 1;
        this.points = tag.getInt("points");
        this.totalPointsEarned = tag.getInt("totalPoints");

        // Load perks
        chosenPerks.clear();
        ListTag perkList = tag.getList("perks", Tag.TAG_STRING);
        for (int i = 0; i < perkList.size(); i++) {
            chosenPerks.add(perkList.getString(i));
        }

        // Load counters
        blockCounters.clear();
        CompoundTag countersTag = tag.getCompound("counters");
        for (String key : countersTag.getAllKeys()) {
            blockCounters.put(key, countersTag.getInt(key));
        }
    }

    @Override
    public String toString() {
        return String.format("%s Lv.%d (%d/%d pts)",
                skillType.getDisplayName(), level, points, getPointsToNextLevel());
    }
}
