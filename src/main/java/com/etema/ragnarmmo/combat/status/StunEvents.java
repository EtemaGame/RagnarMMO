package com.etema.ragnarmmo.combat.status;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Minimal stun framework for the combat domain.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class StunEvents {
    public static final String STUN_UNTIL_TAG = "ragnarmmo_stunned_until";

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && !(direct instanceof LivingEntity)) {
            return;
        }

        Entity attackerEntity = direct != null ? direct : event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        long until = attacker.getPersistentData().getLong(STUN_UNTIL_TAG);
        if (until <= 0) {
            return;
        }

        long now = attacker.level().getGameTime();
        if (now >= until) {
            attacker.getPersistentData().remove(STUN_UNTIL_TAG);
            return;
        }

        event.setCanceled(true);
    }
}
