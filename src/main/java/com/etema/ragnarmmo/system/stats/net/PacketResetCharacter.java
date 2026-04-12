package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.system.stats.progression.StatCost;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketResetCharacter {

    public PacketResetCharacter() {
    }

    public PacketResetCharacter(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void encode(PacketResetCharacter msg, FriendlyByteBuf buf) {
        msg.encode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;

            com.etema.ragnarmmo.system.stats.service.CharacterResetService.resetAllocatedStats(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
