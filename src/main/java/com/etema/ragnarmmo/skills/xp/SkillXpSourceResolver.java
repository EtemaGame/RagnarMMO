package com.etema.ragnarmmo.skills.xp;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skills.runtime.SourceConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillXpSourceResolver {
    private static final Set<ResourceLocation> LOGGED_COMBAT_FALLBACKS = ConcurrentHashMap.newKeySet();

    private SkillXpSourceResolver() {
    }

    public static int resolveCombatXp(LivingEntity target, ResourceLocation skillId) {
        int configuredXp = SourceConfig.getInstance().getXp(target, skillId);
        if (configuredXp != 0) {
            return configuredXp;
        }

        int fallbackXp = SkillXpConfig.combatFallbackXp();
        if (fallbackXp > 0 && LOGGED_COMBAT_FALLBACKS.add(skillId)) {
            RagnarMMO.LOGGER.debug("Using combat skill XP fallback {} for {}", fallbackXp, skillId);
        }
        return fallbackXp;
    }
}
