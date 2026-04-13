package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.net.ClientboundRagnarCombatResultPacket;
import com.etema.ragnarmmo.common.net.Network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Centralized feedback dispatch for the first combat-engine migration slice.
 */
public class RagnarCombatFeedbackService {

    public void sendBasicAttackFeedback(ServerPlayer attacker, LivingEntity target, CombatResolution resolution) {
        if (resolution == null) {
            return;
        }

        if (attacker != null) {
            Network.sendToPlayer(attacker, new ClientboundRagnarCombatResultPacket(
                    resolution.attackerId(),
                    resolution.targetId(),
                    resolution.resultType(),
                    resolution.finalAmount(),
                    resolution.critical()));
        }

        Entity trackingBasis = target != null ? target : attacker;
        if (trackingBasis != null) {
            Network.sendTrackingEntityAndSelf(trackingBasis, new ClientboundRagnarCombatResultPacket(
                    resolution.attackerId(),
                    resolution.targetId(),
                    resolution.resultType(),
                    resolution.finalAmount(),
                    resolution.critical()));
        }

        if (attacker != null && (resolution.resultType() == CombatHitResultType.MISS || resolution.resultType() == CombatHitResultType.DODGE)) {
            attacker.sendSystemMessage(Component.literal("§7Miss"));
        }
    }
}
