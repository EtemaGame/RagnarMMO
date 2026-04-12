package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;

import java.util.Set;

/**
 * Increase HP Recovery — Passive
 * RO: +5 HP regen per level while resting + boosts HP from healing items.
 *
 * Minecraft adaptation:
 *  - Every 10 seconds, heals while standing still.
 *  - Regen formula follows the shared table:
 *    (5 x level) + (0.2% Max HP x level).
 *  - Healing consumables get +10% restorative value per level.
 */
public class IncreaseHpRecoverySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "increase_hp_recovery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK, TriggerType.ITEM_USE_FINISH);
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        var defOpt = SkillRegistry.get(ID);
        int intervalTicks = defOpt
                .map(def -> def.getLevelInt("interval_ticks", level, 200))
                .orElse(200);
        if (player.tickCount % Math.max(1, intervalTicks) != 0) return;
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || !player.onGround()) return;

        float flat = defOpt
                .map(def -> (float) def.getLevelDouble("hp_recovery_flat", level, 5.0D * level))
                .orElse(5.0f * level);
        float maxHpRatio = defOpt
                .map(def -> (float) def.getLevelDouble("hp_recovery_max_hp_ratio", level, 0.002D * level))
                .orElse(0.002f * level);
        float healAmount = flat + (float) (player.getMaxHealth() * maxHpRatio);
        player.heal(healAmount);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.8, player.getZ(),
                    3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    @Override
    public void onItemUseFinish(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        ItemStack stack = event.getItem();
        float bonusHeal = 0.0f;
        float itemHealBonusRatio = SkillRegistry.get(ID)
                .map(def -> (float) def.getLevelDouble("item_heal_bonus_ratio", level, 0.10D * level))
                .orElse(0.10f * level);

        if (stack.isEdible()) {
            var food = stack.getFoodProperties(player);
            if (food != null) {
                bonusHeal += food.getNutrition() * itemHealBonusRatio;
            }
        }

        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (effect.getEffect() == MobEffects.HEAL) {
                float baseHeal = 4.0f * (1 << effect.getAmplifier());
                bonusHeal += baseHeal * itemHealBonusRatio;
            }
        }

        if (bonusHeal > 0.0f) {
            player.heal(bonusHeal);
        }
    }
}
