package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.skills.api.SkillTier;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.data.tree.SkillNode;
import com.etema.ragnarmmo.skills.data.tree.SkillTreeDefinition;
import com.etema.ragnarmmo.skills.data.tree.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SkillTreeAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeAdapter.class);

    public static class SkillNodeWrapper {
        private final ResourceLocation skillId;
        private final ISkillDefinition definition;
        private final int gridX;
        private final int gridY;

        public SkillNodeWrapper(ResourceLocation skillId, ISkillDefinition definition, int gridX, int gridY) {
            this.skillId = skillId;
            this.definition = definition;
            this.gridX = gridX;
            this.gridY = gridY;
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

    public static List<SkillNodeWrapper> getVisibleSkills(JobType job, int tier) {
        List<SkillNodeWrapper> result = new ArrayList<>();

        String jobName = tier == 0 ? "NOVICE" : job.name();
        int treeTier = tier == 0 ? 1 : tier;

        Optional<SkillTreeDefinition> treeOpt = SkillTreeRegistry.getForJob(jobName, treeTier);

        if (treeOpt.isPresent()) {
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

            result.sort((a, b) -> {
                int cmp = Integer.compare(a.getGridY(), b.getGridY());
                return cmp != 0 ? cmp : Integer.compare(a.getGridX(), b.getGridX());
            });

        } else {
            LOGGER.debug("No tree found for job={} tier={}, using registry skill order", jobName, treeTier);
            result = getRegistrySkills(job, tier);
        }

        return result;
    }

    private static List<SkillNodeWrapper> getRegistrySkills(JobType job, int tier) {
        List<SkillNodeWrapper> result = new ArrayList<>();
        Set<ResourceLocation> candidateSkillIds = tier == 0 ? JobType.NOVICE.getAllowedSkillIds() : job.getAllowedSkillIds();

        for (ResourceLocation skillId : candidateSkillIds) {
            var defOpt = SkillRegistry.get(skillId);
            if (defOpt.isEmpty())
                continue;

            ISkillDefinition def = defOpt.get();
            boolean matches = false;

            if (tier == 0) {
                matches = def.getTier() == SkillTier.NOVICE;
            } else if (tier == 1) {
                matches = def.getTier() == SkillTier.FIRST;
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
