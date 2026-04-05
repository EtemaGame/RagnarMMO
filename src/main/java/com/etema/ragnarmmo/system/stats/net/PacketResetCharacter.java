package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
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
                // Reset Job and Levels
                stats.setJobId("ragnarmmo:novice");
                stats.setLevel(1);
                stats.setJobLevel(1);
                stats.setExp(0);
                stats.setJobExp(0);

                // Reset Points
                stats.setStatPoints(0); // Optionally give starting points? Usually 48 or config based.
                // Let's check config or just set to 0 and let ensureBaseStatBaseline handle it
                // if it runs?
                // Actually, if we reset, we might want to re-grant base points.
                // For now, let's look at PlayerStats.java logic.
                // Using 48 as safe default for now or 0 if we assume fresh start logic handles
                // it.
                // The user said "reinicia a 0 todo".
                stats.setStatPoints(0);
                stats.setSkillPoints(0);

                // Reset Stats to 1
                for (StatKeys key : StatKeys.values()) {
                    stats.set(key, 1);
                }

                // We might need to reset the "baseStatPointsGranted" flag if we want them to
                // get the start bonus again.
                // But PlayerStats doesn't expose a setter for that easily in the interface,
                // wait, I saw setBaseStatPointsGranted in the file view.
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






