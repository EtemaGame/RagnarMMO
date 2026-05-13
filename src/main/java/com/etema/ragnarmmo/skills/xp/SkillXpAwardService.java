package com.etema.ragnarmmo.skills.xp;

import com.etema.ragnarmmo.skills.api.XPGainReason;
import com.etema.ragnarmmo.skills.resolver.JobSkillEligibilityService;
import com.etema.ragnarmmo.skills.resolver.WeaponSkillResolver;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SkillXpAwardService {
    private SkillXpAwardService() {
    }

    public static void awardCombatXp(ServerPlayer player, LivingEntity target) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            for (ResourceLocation skillId : WeaponSkillResolver.applicableCombatSkills(player)) {
                if (!JobSkillEligibilityService.canReceiveCombatXp(skillId)) {
                    continue;
                }

                int xp = SkillXpSourceResolver.resolveCombatXp(target, skillId);
                if (xp > 0) {
                    skills.addXP(skillId, xp, XPGainReason.COMBAT_PVE);
                }
            }
        });
    }
}
