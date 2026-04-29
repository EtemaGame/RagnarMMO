package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Spear Mastery — Passive
 * RO: +4 ATK per level when wielding Spears.
 * Minecraft: +3% damage per level when the player holds a TridentItem (vanilla spear proxy).
 */
public class SpearMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "spear_mastery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
