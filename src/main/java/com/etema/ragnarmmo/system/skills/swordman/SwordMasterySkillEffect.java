package com.etema.ragnarmmo.system.skills.swordman;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/**
 * Sword Mastery — Passive
 * RO: +4 ATK per level when wielding 1-Handed Swords or Daggers.
 * Minecraft: +2% damage per level added to incoming damage on offensive hits via AttributeModifier.
 *  
 * Applied via onOffensiveHurt so it only triggers when the player is the attacker.
 */
public class SwordMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sword_mastery");
    private static final UUID MODIFIER_UUID = UUID.fromString("b3c4d5e6-0001-0001-0001-000000000001");
    private static final String MODIFIER_NAME = "ragnarmmo:sword_mastery_dmg";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        // Only apply when holding a SwordItem or Dagger (SwordItem subtypes)
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof net.minecraft.world.item.SwordItem)) return;

        // +2% damage per level (max +20% at level 10)
        float bonus = event.getAmount() * (0.02f * level);
        event.setAmount(event.getAmount() + bonus);
    }
}
