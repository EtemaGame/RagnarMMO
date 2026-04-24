package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages all Life Skills for a single player.
 * Uses point-based progression instead of XP.
 *
 * Uses LifeSkillType as the only public life-skill identifier.
 */
public class LifeSkillManager {

    private final EnumMap<LifeSkillType, LifeSkillProgress> skills;
    private Player player;

    public LifeSkillManager() {
        this.skills = new EnumMap<>(LifeSkillType.class);
        for (LifeSkillType type : LifeSkillType.values()) {
            skills.put(type, new LifeSkillProgress(type));
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    // ==================== NEW API (LifeSkillType) ====================

    /**
     * Get progress for a specific life skill.
     */
    public LifeSkillProgress getSkill(LifeSkillType type) {
        return skills.get(type);
    }

    /**
     * Get level for a specific life skill.
     */
    public int getLevel(LifeSkillType type) {
        LifeSkillProgress progress = skills.get(type);
        return progress != null ? progress.getLevel() : 0;
    }

    /**
     * Add points to a life skill.
     * 
     * @return Number of levels gained
     */
    public int addPoints(LifeSkillType type, int amount) {
        LifeSkillProgress progress = skills.get(type);
        if (progress == null)
            return 0;
        return progress.addPoints(amount);
    }

    /**
     * Increment block counter and add earned points.
     * Used for "per X blocks" sources.
     * 
     * @return Number of levels gained
     */
    public int processBlockBreak(LifeSkillType type, String blockId, int threshold, int pointsPerThreshold) {
        LifeSkillProgress progress = skills.get(type);
        if (progress == null)
            return 0;

        int pointsEarned = progress.incrementBlockCounter(blockId, threshold, pointsPerThreshold);
        if (pointsEarned > 0) {
            return progress.addPoints(pointsEarned);
        }
        return 0;
    }

    /**
     * Check if player has a pending perk choice for any life skill.
     */
    public LifeSkillType getPendingPerkSkill() {
        for (LifeSkillType type : LifeSkillType.values()) {
            LifeSkillProgress progress = skills.get(type);
            if (progress != null && progress.hasPendingPerkChoice()) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get all life skill types.
     */
    public static LifeSkillType[] getLifeSkillTypes() {
        return LifeSkillType.values();
    }

    /**
     * Get total level across all life skills.
     */
    public int getTotalLevel() {
        int total = 0;
        for (LifeSkillProgress progress : skills.values()) {
            total += progress.getLevel();
        }
        return total;
    }

    // ==================== NBT Serialization ====================

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<LifeSkillType, LifeSkillProgress> entry : skills.entrySet()) {
            tag.put(entry.getKey().getId(), entry.getValue().serializeNBT());
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (LifeSkillType type : LifeSkillType.values()) {
            if (tag.contains(type.getId())) {
                LifeSkillProgress progress = skills.get(type);
                if (progress != null) {
                    progress.deserializeNBT(tag.getCompound(type.getId()));
                }
            }
        }
    }
}
