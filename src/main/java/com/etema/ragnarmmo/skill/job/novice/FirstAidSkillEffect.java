package com.etema.ragnarmmo.skill.job.novice;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * First Aid - Passive (Novice)
 * RO behavior: Natural HP recovery is increased.
 * Minecraft: Passive regeneration while not in combat (hurtTime == 0).
 */
import java.util.Set;

/**
 * First Aid - Passive (Novice)
 * RO behavior: Natural HP recovery is increased.
 * Minecraft: Passive regeneration while not in combat (hurtTime == 0).
 */
public class FirstAidSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "first_aid");

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK, TriggerType.ITEM_USE_FINISH);
    }

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void onItemUseFinish(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event, ServerPlayer player, int level) {
        if (level <= 0) return;
        
        net.minecraft.world.item.ItemStack stack = event.getItem();
        if (!stack.isEdible()) return;

        net.minecraft.world.food.FoodProperties food = stack.getFoodProperties(player);
        if (food == null) return;

        if (player.getHealth() >= player.getMaxHealth()) return;

        // Bonus healing from food: +10% per level (based on nutrition value)
        float heal = (float) (food.getNutrition() * 0.10f * level);
        if (heal > 0) {
            player.heal(heal);
        }
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
