package com.etema.ragnarmmo.skill.data.tree;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents a single skill node in a skill tree layout.
 * Immutable data class storing position and visual information.
 */
public final class SkillNode {
    private final ResourceLocation skillId;
    private final int gridX;
    private final int gridY;

    public SkillNode(ResourceLocation skillId, int gridX, int gridY) {
        this.skillId = skillId;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public ResourceLocation getSkillId() {
        return skillId;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    @Override
    public String toString() {
        return "SkillNode{" +
                "skillId=" + skillId +
                ", gridX=" + gridX +
                ", gridY=" + gridY +
                '}';
    }
}
