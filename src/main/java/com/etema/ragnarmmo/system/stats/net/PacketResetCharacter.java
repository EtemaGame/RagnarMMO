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

            RagnarCoreAPI.get(player).ifPresent(stats -> {
                // Refund points spent on stats
                int totalRefunded = 0;
                for (StatKeys key : StatKeys.values()) {
                    int currentVal = stats.get(key);
                    // Standard RO refund logic: refund cost of each level spent
                    for (int v = 1; v < currentVal; v++) {
                        totalRefunded += StatCost.costToIncrease(v);
                    }
                    stats.set(key, 1);
                }

                // Add refunded points to pool
                stats.setStatPoints(stats.getStatPoints() + totalRefunded);

                // Reset base stat baseline if needed (start points)
                if (stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats implementation) {
                    implementation.setBaseStatPointsGranted(false);
                    implementation.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
                }

                PlayerStatsSyncService.sync(player, stats);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
