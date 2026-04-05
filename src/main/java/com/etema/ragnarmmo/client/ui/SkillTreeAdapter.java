package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.api.SkillType;
import com.etema.ragnarmmo.skill.api.SkillTier;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.data.tree.SkillNode;
import com.etema.ragnarmmo.skill.data.tree.SkillTreeDefinition;
import com.etema.ragnarmmo.skill.data.tree.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Adapter to bridge between the new data-driven skill tree system
 * and the existing SkillsScreen UI that expects SkillType enums.
 *
 * This provides a gradual migration path - the UI can continue to work
 * with SkillType while we transition to ResourceLocation-based IDs.
 */
public class SkillTreeAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeAdapter.class);

    /**
     * Wrapper that provides SkillType-like interface for dynamic skills.
     */
    public static class SkillNodeWrapper {
        private final ResourceLocation skillId;
        private final ISkillDefinition definition;
        private final int gridX;
        private final int gridY;
        private final SkillType legacyType; // Null if no legacy enum exists

        public SkillNodeWrapper(ResourceLocation skillId, ISkillDefinition definition, int gridX, int gridY) {
            this.skillId = skillId;
            this.definition = definition;
            this.gridX = gridX;
            this.gridY = gridY;
            this.legacyType = SkillType.fromResourceLocation(skillId);
        }

        public ResourceLocation getSkillId() {
            return skillId;
        }

        public ISkillDefinition getDefinition() {
            return definition;
        }

        public int getGridX() {
            return gridX;
        }

        public int getGridY() {
            return gridY;
        }

        /**
         * Get the legacy SkillType enum if it exists.
         * Returns null for new skills that don't have enum entries.
         */
        public SkillType getLegacyType() {
            return legacyType;
        }

        /**
         * Check if this skill has a legacy enum representation.
         */
        public boolean hasLegacyType() {
            return legacyType != null;
        }

        public Map<ResourceLocation, Integer> getRequirements() {
            return definition.getRequirements();
        }

        @Override
        public String toString() {
            return "SkillNodeWrapper{" +
                    "skillId=" + skillId +
                    ", gridPos=(" + gridX + "," + gridY + ")" +
                    '}';
        }
    }

    /**
     * Get visible skills for a job and tier using the new skill tree system.
     * Falls back to legacy enum-based system if no tree layout is found.
     *
     * @param job  The player's job
     * @param tier The skill tier (1 = NOVICE/FIRST, 2 = SECOND, etc.)
     * @return List of skill wrappers
     */
    public static List<SkillNodeWrapper> getVisibleSkills(JobType job, int tier) {
        List<SkillNodeWrapper> result = new ArrayList<>();

        // Try to get tree from registry
        String jobName = tier == 0 ? "NOVICE" : job.name();
        int treeTier = tier == 0 ? 1 : tier;

        Optional<SkillTreeDefinition> treeOpt = SkillTreeRegistry.getForJob(jobName, treeTier);

        if (treeOpt.isPresent()) {
            // Use dynamic tree system
            SkillTreeDefinition tree = treeOpt.get();
            List<SkillNode> allNodes = tree.getAllSkills();

            for (SkillNode node : allNodes) {
                ResourceLocation skillId = node.getSkillId();
                Optional<ISkillDefinition> defOpt = SkillRegistry.get(skillId).map(def -> (ISkillDefinition) def);

                if (defOpt.isPresent()) {
                    result.add(new SkillNodeWrapper(
                            skillId,
                            defOpt.get(),
                            node.getGridX(),
                            node.getGridY()));
                } else {
                    LOGGER.warn("Skill {} in tree but not found in registry", skillId);
                }
            }

            // Sort by grid position
            result.sort((a, b) -> {
                int cmp = Integer.compare(a.getGridY(), b.getGridY());
                return cmp != 0 ? cmp : Integer.compare(a.getGridX(), b.getGridX());
            });

        } else {
            // Fallback to legacy enum-based system
            LOGGER.debug("No tree found for job={} tier={}, using legacy enum system", jobName, treeTier);
            result = getLegacySkills(job, tier);
        }

        return result;
    }

    /**
     * Fallback: Get skills using the old SkillType enum system.
     */
    @SuppressWarnings("deprecation")
    private static List<SkillNodeWrapper> getLegacySkills(JobType job, int tier) {
        List<SkillNodeWrapper> result = new ArrayList<>();

        for (SkillType type : SkillType.values()) {
            ResourceLocation skillId = type.toResourceLocation();
            var defOpt = SkillRegistry.get(skillId);
            if (defOpt.isEmpty())
                continue;

            ISkillDefinition def = defOpt.get();
            boolean matches = false;

            if (tier == 0) {
                matches = def.getTier() == SkillTier.NOVICE;
            } else if (tier == 1) {
                matches = def.getTier() == SkillTier.FIRST &&
                        job.getAllowedSkills().contains(type);
            }

            if (matches) {
                result.add(new SkillNodeWrapper(
                        skillId,
                        def,
                        def.getGridX(),
                        def.getGridY()));
            }
        }

        result.sort((a, b) -> {
            int cmp = Integer.compare(a.getGridY(), b.getGridY());
            return cmp != 0 ? cmp : Integer.compare(a.getGridX(), b.getGridX());
        });

        return result;
    }
}
