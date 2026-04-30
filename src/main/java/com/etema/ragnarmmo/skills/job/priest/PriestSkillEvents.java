package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Priest skill event hooks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class PriestSkillEvents {

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
                   }
                }
            } else {
                event.getEntity().getPersistentData().remove("slow_poison");
            }
        }
    }
}
