package com.etema.ragnarmmo.skill.job.knight;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
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

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "spear_mastery");

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
        if (level <= 0) return;

        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        boolean isSpear = held.getItem() instanceof TridentItem
                || held.getTags().anyMatch(t -> t.location().toString().contains("ragnarmmo:spear"));

        if (!isSpear) return;

        // +3% damage per level (higher than sword since spears have lower ASPD in RO)
        float bonus = event.getAmount() * (0.03f * level);
        event.setAmount(event.getAmount() + bonus);
    }
}
