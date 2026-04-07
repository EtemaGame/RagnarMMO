package com.etema.ragnarmmo.combat.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.etema.ragnarmmo.combat.api.CombatActionType.SKILL;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatTargetCandidate;
import com.etema.ragnarmmo.combat.engine.RagnarCombatEngine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ServerboundRagnarSkillUsePacket {
    private final int sequenceId;
    private final String skillId;
    private final int skillLevel;
    private final int selectedSlot;
    private final boolean offHand;
    private final int[] candidateTargetIds;

    public ServerboundRagnarSkillUsePacket(int sequenceId, String skillId, int skillLevel, int selectedSlot, boolean offHand, int[] candidateTargetIds) {
        this.sequenceId = sequenceId;
        this.skillId = skillId == null ? "" : skillId;
        this.skillLevel = skillLevel;
        this.selectedSlot = selectedSlot;
        this.offHand = offHand;
        this.candidateTargetIds = candidateTargetIds == null ? new int[0] : candidateTargetIds;
    }

    public ServerboundRagnarSkillUsePacket(FriendlyByteBuf buf) {
        this.sequenceId = buf.readInt();
        this.skillId = buf.readUtf();
        this.skillLevel = buf.readInt();
        this.selectedSlot = buf.readInt();
        this.offHand = buf.readBoolean();
        this.candidateTargetIds = buf.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(sequenceId);
        buf.writeUtf(skillId);
        buf.writeInt(skillLevel);
        buf.writeInt(selectedSlot);
        buf.writeBoolean(offHand);
        buf.writeVarIntArray(candidateTargetIds);
    }

    public static void handle(ServerboundRagnarSkillUsePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            if (msg.candidateTargetIds.length > 20) {
                return;
            }

            List<com.etema.ragnarmmo.combat.api.CombatTargetCandidate> candidates = new ArrayList<>(msg.candidateTargetIds.length);
            for (int id : msg.candidateTargetIds) {
                candidates.add(new com.etema.ragnarmmo.combat.api.CombatTargetCandidate(id, "domain", 0.0D, false));
            }

            RagnarCombatEngine.get().handleSkillUseRequest(new CombatRequestContext(
                    player,
                    com.etema.ragnarmmo.combat.api.CombatActionType.SKILL,
                    msg.sequenceId,
                    0,
                    msg.offHand,
                    msg.selectedSlot,
                    msg.skillId,
                    candidates,
                    java.util.Map.of("level", msg.skillLevel)));
        });
        ctx.setPacketHandled(true);
    }
}
