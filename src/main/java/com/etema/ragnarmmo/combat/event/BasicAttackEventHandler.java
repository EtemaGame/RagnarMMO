package com.etema.ragnarmmo.combat.event;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.api.RagnarAttackRequest;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import com.etema.ragnarmmo.combat.api.RagnarTargetSource;
import com.etema.ragnarmmo.combat.engine.RagnarCombatEngine;

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

        event.setCanceled(true);
        RagnarCombatEngine.get().processBasicAttackRequest(attacker, new RagnarAttackRequest(
                attacker.tickCount,
                0,
                false,
                attacker.getInventory().selected,
                java.util.List.of(RagnarTargetCandidate.from(target.getId(), RagnarTargetSource.SERVER_RESOLVED))));
    }
}
