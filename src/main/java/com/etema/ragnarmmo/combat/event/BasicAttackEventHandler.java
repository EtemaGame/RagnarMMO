package com.etema.ragnarmmo.combat.event;

import com.etema.ragnarmmo.RagnarMMO;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First practical hook that routes basic attacks through RagnarMMO on the
 * server. This intentionally cancels vanilla melee authority for player vs
 * living-entity attacks while the packet path remains the functional authority.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BasicAttackEventHandler {
    private BasicAttackEventHandler() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        Entity rawTarget = event.getTarget();
        if (!(rawTarget instanceof LivingEntity target)) {
            return;
        }

        // RagnarMMO must be the combat authority for this slice.
        event.setCanceled(true);
    }
}
