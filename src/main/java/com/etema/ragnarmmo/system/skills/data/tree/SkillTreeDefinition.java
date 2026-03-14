package com.etema.ragnarmmo.system.skills.data.tree;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a skill tree layout for a specific job and tier.
 * Immutable data class loaded from JSON at runtime.
 *
 * Example JSON location: data/ragnarmmo/skill_trees/mage_1.json
 */
public final class SkillTreeDefinition {
    private final ResourceLocation id;
    private final String job;              // e.g., "MAGE", "SWORDSMAN"
    private final int tier;                // 1 for first job, 2 for second job, etc.
    private final int gridWidth;           // Max grid columns (e.g., 10)
    private final int gridHeight;          // Max grid rows (e.g., 7)
    private final List<ResourceLocation> inheritFrom;  // Layouts to inherit from
    private final List<SkillNode> skills;  // Skill nodes in this tree

    private SkillTreeDefinition(Builder builder) {
        this.id = builder.id;
        this.job = builder.job;
        this.tier = builder.tier;
        this.gridWidth = builder.gridWidth;
        this.gridHeight = builder.gridHeight;
        this.inheritFrom = List.copyOf(builder.inheritFrom);
        this.skills = List.copyOf(builder.skills);
    }

    public ResourceLocation getId() {
        return id;
    }

    public String getJob() {
        return job;
    }

    public int getTier() {
        return tier;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public List<ResourceLocation> getInheritFrom() {
        return inheritFrom;
    }

    public List<SkillNode> getSkills() {
        return skills;
    }

    /**
     * Gets all skills including inherited ones from parent trees.
     *
     * @return Combined list of all skill nodes (including inherited)
     */
    public List<SkillNode> getAllSkills() {
        List<SkillNode> all = new ArrayList<>(skills);

        for (ResourceLocation parentId : inheritFrom) {
            SkillTreeRegistry.get(parentId).ifPresent(parent -> {
                all.addAll(parent.getAllSkills());
            });
        }

        return all;
    }

    @Override
    public String toString() {
        return "SkillTreeDefinition{" +
                "id=" + id +
                ", job='" + job + '\'' +
                ", tier=" + tier +
                ", gridSize=" + gridWidth + "x" + gridHeight +
                ", inherits=" + inheritFrom.size() +
                ", skills=" + skills.size() +
                '}';
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static class Builder {
        private final ResourceLocation id;
        private String job = "NOVICE";
        private int tier = 1;
        private int gridWidth = 10;
        private int gridHeight = 7;
        private final List<ResourceLocation> inheritFrom = new ArrayList<>();
        private final List<SkillNode> skills = new ArrayList<>();

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder job(String job) {
            this.job = job;
            return this;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder gridSize(int width, int height) {
            this.gridWidth = width;
            this.gridHeight = height;
            return this;
        }

        public Builder inheritFrom(ResourceLocation parentTreeId) {
            this.inheritFrom.add(parentTreeId);
            return this;
        }

        public Builder addSkill(ResourceLocation skillId, int gridX, int gridY) {
            this.skills.add(new SkillNode(skillId, gridX, gridY));
            return this;
        }

        public Builder addSkill(SkillNode node) {
            this.skills.add(node);
            return this;
        }

        public SkillTreeDefinition build() {
            return new SkillTreeDefinition(this);
        }
    }
}
