package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Priest skill event hooks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class PriestSkillEvents {

    /**
     * Mace Mastery Hook: Increases damage when using Axe or Pickaxe (Mace equivalents).
     */
    @SubscribeEvent
    public static void onMaceMasteryHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int level = player.getPersistentData().getInt("mace_mastery_level");
            if (level > 0) {
                ItemStack held = player.getMainHandItem();
                if (held.getItem() instanceof AxeItem || held.getItem() instanceof PickaxeItem) {
                    // +2% damage per level
                    float bonus = 1.0f + (level * 0.02f);
                    event.setAmount(event.getAmount() * bonus);
                }
            }
        }
    }

    /**
     * Slow Poison Hook: Prevents death from Poison if Slow Poison is active.
     */
    @SubscribeEvent
    public static void onSlowPoisonDamage(LivingDamageEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("slow_poison")) {
            long expiry = event.getEntity().getPersistentData().getLong("slow_poison_expiry");
            if (expiry > event.getEntity().level().getGameTime()) {
                // If it's POISON damage and it would kill
                if (event.getSource().getMsgId().equals("magic") || event.getSource().getMsgId().equals("poison")) {
                   float currentHealth = event.getEntity().getHealth();
                   if (event.getAmount() >= currentHealth) {
                       // Cap damage to leave 1 HP
                       event.setAmount(Math.max(0, currentHealth - 1.0f));
                   }
                }
            } else {
                event.getEntity().getPersistentData().remove("slow_poison");
            }
        }
    }
}
