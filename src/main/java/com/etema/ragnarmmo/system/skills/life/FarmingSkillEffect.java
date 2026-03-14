package com.etema.ragnarmmo.system.skills.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.system.skills.ISkillEffect;

/**
 * Effect handler for Farming skill.
 * Provides increased crop yield and farming efficiency when using hoes.
 * Note: Crop growth speed and yield bonuses would require additional event handling
 * for BlockGrowEvent and HarvestDropsEvent which are not implemented here.
 */
public class FarmingSkillEffect implements ISkillEffect {

    @Override
    public ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "farming");
    }

    /**
     * Farming skill effects are primarily event-based:
     * - Increased crop drops on harvest (requires HarvestDropsEvent)
     * - Faster crop growth (requires BlockGrowEvent)
     * - Chance for bonus seeds (requires HarvestDropsEvent)
     *
     * These would need to be implemented in a separate event handler
     * that listens to world/block events.
     */
}
