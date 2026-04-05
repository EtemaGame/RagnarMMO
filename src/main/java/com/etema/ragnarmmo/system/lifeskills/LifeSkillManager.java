package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.skill.api.SkillType;
import com.etema.ragnarmmo.skill.api.SkillCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages all Life Skills for a single player.
 * Uses point-based progression instead of XP.
 *
 * NOTE: Now uses LifeSkillType enum (separate from SkillType).
 * Legacy SkillType methods kept for migration compatibility.
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

    // ==================== LEGACY API (SkillType) - For Migration
    // ====================

    /**
     * Check if a SkillType is a life skill.
     * LEGACY: Kept for compatibility during migration.
     */
    public static boolean isLifeSkill(SkillType type) {
        if (type == null)
            return false;
        return LifeSkillType.fromId(type.getId()) != null;
    }

    /**
     * Convert legacy SkillType to LifeSkillType.
     * Returns null if not a life skill.
     */
    public static LifeSkillType fromLegacy(SkillType legacyType) {
        if (legacyType == null || !isLifeSkill(legacyType)) {
            return null;
        }
        return LifeSkillType.fromId(legacyType.getId());
    }

    /**
     * Get progress using legacy SkillType.
     * LEGACY: Use getSkill(LifeSkillType) instead.
     */
    public LifeSkillProgress getSkill(SkillType legacyType) {
        LifeSkillType type = fromLegacy(legacyType);
        return type != null ? skills.get(type) : null;
    }

    /**
     * Get level using legacy SkillType.
     * LEGACY: Use getLevel(LifeSkillType) instead.
     */
    public int getLevel(SkillType legacyType) {
        LifeSkillType type = fromLegacy(legacyType);
        return type != null ? getLevel(type) : 0;
    }

    /**
     * Add points using legacy SkillType.
     * LEGACY: Use addPoints(LifeSkillType, int) instead.
     */
    public int addPoints(SkillType legacyType, int amount) {
        LifeSkillType type = fromLegacy(legacyType);
        return type != null ? addPoints(type, amount) : 0;
    }

    /**
     * Process block break using legacy SkillType.
     * LEGACY: Use processBlockBreak(LifeSkillType, ...) instead.
     */
    public int processBlockBreak(SkillType legacyType, String blockId, int threshold, int pointsPerThreshold) {
        LifeSkillType type = fromLegacy(legacyType);
        return type != null ? processBlockBreak(type, blockId, threshold, pointsPerThreshold) : 0;
    }

    // ==================== NBT Serialization ====================

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<LifeSkillType, LifeSkillProgress> entry : skills.entrySet()) {
            // Use getId() which matches legacy SkillType.getId() for compatibility
            tag.put(entry.getKey().getId(), entry.getValue().serializeNBT());
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (LifeSkillType type : LifeSkillType.values()) {
            // Use getId() which matches legacy keys
            if (tag.contains(type.getId())) {
                LifeSkillProgress progress = skills.get(type);
                if (progress != null) {
                    progress.deserializeNBT(tag.getCompound(type.getId()));
                }
            }
        }
    }
}
