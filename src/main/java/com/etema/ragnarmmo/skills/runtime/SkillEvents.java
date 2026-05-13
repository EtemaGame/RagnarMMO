package com.etema.ragnarmmo.skills.runtime;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skills.xp.SkillXpAwardService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles events to award Skill XP.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SkillEvents {
    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            SkillXpAwardService.awardCombatXp(player, event.getEntity());
        }
    }
}
