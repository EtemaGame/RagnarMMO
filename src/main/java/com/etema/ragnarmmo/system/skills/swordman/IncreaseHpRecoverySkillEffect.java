package com.etema.ragnarmmo.system.skills.swordman;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

/**
 * Increase HP Recovery — Passive
 * RO: +5 HP regen per level while resting + boosts HP from healing items.
 *
 * Minecraft: Every 20 ticks (1 second), heal a small amount of HP if the player
 * is not in combat (not hurt recently). The heal scales with the skill level.
 *
 * "Not in combat" is approximated by checking hurtTime == 0.
 * The 10% food healing bonus is handled separately in SkillEffectHandler via
 * onItemUseFinish, but here we also provide the passive regen.
 */
public class IncreaseHpRecoverySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "increase_hp_recovery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        // Only regen if not hurt recently (approximates "resting" state)
        if (player.hurtTime > 0) return;

        // +0.5 HP per level per second (10 ticks intervals = every 0.5s)
        // Scales to 5 HP/s at level 10
        float healAmount = 0.5f * level;
        player.heal(healAmount);
    }
}
