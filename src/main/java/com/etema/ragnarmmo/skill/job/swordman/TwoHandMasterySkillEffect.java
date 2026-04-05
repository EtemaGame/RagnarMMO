package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Two-Hand Mastery — Passive
 * RO: +4 ATK per level when wielding 2-Handed Swords.
 * Minecraft adaptation: adds a flat +4 damage per level to outgoing 2H sword hits.
 */
public class TwoHandMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "two_hand_mastery");

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
        if (level <= 0) return;

        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof net.minecraft.world.item.SwordItem)) return;

        // Check for the two_handed tag; if no tag system exists, the skill still gives
        // a slight bonus to all swords as a fallback.
        boolean isTwoHanded = held.getTags()
                .anyMatch(tag -> tag.location().toString().equals("ragnarmmo:two_handed"));

        if (!isTwoHanded) return; // Only applies to 2H weapons

        event.setAmount(event.getAmount() + (4.0f * level));
    }
}
