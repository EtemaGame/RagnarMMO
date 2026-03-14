package com.etema.ragnarmmo.system.skills.novice;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * First Aid - Passive (Novice)
 * RO behavior: Natural HP recovery is increased.
 * Minecraft: Passive regeneration while not in combat (hurtTime == 0).
 */
public class FirstAidSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "first_aid");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // No longer an active skill
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        // Only regen if not hurt recently (out-of-combat)
        if (player.hurtTime > 0) return;

        // +0.25 HP per level per second (every 1 second tick in SkillEffectHandler)
        // At Level 1: 0.25 HP/s, Level 10: 2.5 HP/s (1.25 hearts/s)
        float healAmount = 0.25f * level;
        player.heal(healAmount);

        // Subtle visual for regen
        if (player.tickCount % 40 == 0) { // Every 2 seconds
            ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    1, 0.2, 0.2, 0.2, 0.05);
        }
    }
}
