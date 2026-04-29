package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Sword Mastery — Passive
 * RO: +4 ATK per level when wielding 1-Handed Swords.
 * Minecraft adaptation: adds a flat +4 damage per level to outgoing sword hits.
 */
public class SwordMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sword_mastery");

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
