package com.etema.ragnarmmo.skills.data.progression;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.skills.data.progression.SkillState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SkillProgressManager {

    /**
     * Helper utility to quickly get a SkillProgress instance for a specific player
     * and skill.
     *
     * @param player  The player
     * @param skillId The skill's resource location
     * @return SkillProgress instance, wrapping the current state or a default empty
     *         state.
     */
    public static SkillProgress getProgress(Player player, ResourceLocation skillId) {
        if (player == null || skillId == null) {
            return new SkillProgress(
                    skillId != null ? skillId : new ResourceLocation("ragnarmmo", "unknown"));
        }

        return com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider.get(player).map(manager -> {
            SkillState state = manager.getSkillState(skillId);
            if (state != null) {
                return new SkillProgress(skillId, state);
            }
            return new SkillProgress(skillId);
        }).orElse(new SkillProgress(skillId));
    }
}
