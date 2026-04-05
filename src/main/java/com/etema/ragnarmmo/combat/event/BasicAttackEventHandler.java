package com.etema.ragnarmmo.combat.event;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.engine.RagnarBasicAttackService;
import com.etema.ragnarmmo.combat.engine.RagnarCombatFeedbackService;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First practical hook that routes basic attacks through RagnarMMO on the
 * server. This intentionally cancels vanilla melee authority for player vs
 * living-entity attacks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BasicAttackEventHandler {
    private static final RagnarBasicAttackService BASIC_ATTACKS = new RagnarBasicAttackService();
    private static final RagnarCombatFeedbackService FEEDBACK = new RagnarCombatFeedbackService();

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

        var resolution = BASIC_ATTACKS.tryResolve(attacker, target);
        if (resolution == null) {
            CombatDebugLog.logValidationReject(
                    new com.etema.ragnarmmo.combat.api.CombatRequestContext(
                            attacker,
                            com.etema.ragnarmmo.combat.api.CombatActionType.BASIC_ATTACK,
                            -1,
                            0,
                            false,
                            null,
                            java.util.List.of()),
                    "basic_attack_service_returned_null");
            return;
        }

        FEEDBACK.sendBasicAttackFeedback(attacker, target, resolution);
    }
}
