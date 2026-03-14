package com.etema.ragnarmmo.system.skills;

import com.etema.ragnarmmo.RagnarMMO;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Minimal stun framework:
 * - Skills can mark entities with a "stunned until" game-time.
 * - While stunned, outgoing attacks are canceled.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class StunEvents {
    public static final String STUN_UNTIL_TAG = "ragnarmmo_stunned_until";

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && !(direct instanceof LivingEntity)) {
            // Projectiles (arrows, etc.) should still deal damage even if the shooter later gets stunned.
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

