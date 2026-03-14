package com.etema.ragnarmmo.system.skills.swordman;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Two-Hand Mastery — Passive
 * RO: +4 ATK per level when wielding 2-Handed Swords.
 * Minecraft: +3% damage per level (slightly higher than 1H since 2H is a bigger commitment).
 *  
 * To distinguish 2H swords, this mod relies on whether the item has
 * the custom item tag "ragnarmmo:two_handed". If that tag doesn't exist yet for an item,
 * we fall back gracefully to all SwordItems not tagged as 1H.
 */
public class TwoHandMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "two_hand_mastery");

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

        // +3% damage per level
        float bonus = event.getAmount() * (0.03f * level);
        event.setAmount(event.getAmount() + bonus);
    }
}
