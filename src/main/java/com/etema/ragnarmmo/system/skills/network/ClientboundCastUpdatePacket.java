package com.etema.ragnarmmo.system.skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundCastUpdatePacket {
    private final String skillId;
    private final int currentTicks;
    private final int totalTicks;

    public ClientboundCastUpdatePacket(String skillId, int currentTicks, int totalTicks) {
        this.skillId = skillId != null ? skillId : "";
        this.currentTicks = currentTicks;
        this.totalTicks = totalTicks;
    }

    public ClientboundCastUpdatePacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf();
        this.currentTicks = buf.readInt();
        this.totalTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
        buf.writeInt(currentTicks);
        buf.writeInt(totalTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler
                        .handleCastUpdate(skillId, currentTicks, totalTicks)));
        context.get().setPacketHandled(true);
    }
}
