package com.etema.ragnarmmo.combat.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.etema.ragnarmmo.combat.api.RagnarAttackRequest;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import com.etema.ragnarmmo.combat.api.RagnarTargetSource;
import com.etema.ragnarmmo.combat.api.BasicAttackSource;
import com.etema.ragnarmmo.combat.engine.RagnarCombatEngine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ServerboundRagnarBasicAttackPacket {
    private final int sequenceId;
    private final int comboIndex;
    private final boolean offHand;
    private final int selectedSlot;
    private final int[] candidateTargetIds;

    public ServerboundRagnarBasicAttackPacket(int sequenceId, int comboIndex, boolean offHand, int selectedSlot,
            int[] candidateTargetIds) {
        this.sequenceId = sequenceId;
        this.comboIndex = comboIndex;
        this.offHand = offHand;
        this.selectedSlot = selectedSlot;
        this.candidateTargetIds = candidateTargetIds == null ? new int[0] : candidateTargetIds;
    }

    public ServerboundRagnarBasicAttackPacket(FriendlyByteBuf buf) {
        this.sequenceId = buf.readInt();
        this.comboIndex = buf.readInt();
        this.offHand = buf.readBoolean();
        this.selectedSlot = buf.readInt();
        this.candidateTargetIds = buf.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(sequenceId);
        buf.writeInt(comboIndex);
        buf.writeBoolean(offHand);
        buf.writeInt(selectedSlot);
        buf.writeVarIntArray(candidateTargetIds);
    }

    public static void handle(ServerboundRagnarBasicAttackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            // basic payload protection
            if (msg.candidateTargetIds.length > 20) {
                return;
            }

            List<RagnarTargetCandidate> candidates = new ArrayList<>(msg.candidateTargetIds.length);
            for (int id : msg.candidateTargetIds) {
                candidates.add(RagnarTargetCandidate.from(id, RagnarTargetSource.CLIENT_AIM));
            }

            RagnarAttackRequest request = new RagnarAttackRequest(
                    msg.sequenceId,
                    msg.comboIndex,
                    msg.offHand,
                    msg.selectedSlot,
                    candidates);

            RagnarCombatEngine.get().processBasicAttackRequest(player, request, BasicAttackSource.CLIENT_PACKET);
        });
        ctx.setPacketHandled(true);
    }
}
