package com.etema.ragnarmmo.combat.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundRagnarCastStatePacket {
    public enum CastState {
        STARTED,
        INTERRUPTED,
        COMPLETED,
        FAILED
    }

    private final int casterId;
    private final String skillId;
    private final CastState state;
    private final int durationTicks;

    public ClientboundRagnarCastStatePacket(int casterId, String skillId, CastState state, int durationTicks) {
        this.casterId = casterId;
        this.skillId = skillId == null ? "" : skillId;
        this.state = state;
        this.durationTicks = durationTicks;
    }

    public ClientboundRagnarCastStatePacket(FriendlyByteBuf buf) {
        this.casterId = buf.readInt();
        this.skillId = buf.readUtf();
        this.state = CastState.values()[buf.readVarInt()];
        this.durationTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(casterId);
        buf.writeUtf(skillId);
        buf.writeVarInt(state.ordinal());
        buf.writeInt(durationTicks);
    }

    public static void handle(ClientboundRagnarCastStatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Reserved for future client cast-state integration.
        });
        ctx.setPacketHandled(true);
    }
}
