package com.etema.ragnarmmo.system.stats.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs the full PlayerSkills capability (levels/xp/cart) to the client.
 * This avoids client-side "all skills at 0" issues on login/respawn.
 */
public class ClientboundSkillSyncPacket {
    private final CompoundTag data;

    public ClientboundSkillSyncPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public ClientboundSkillSyncPacket(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.data = tag == null ? new CompoundTag() : tag;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleSkillSync(data)));
        ctx.get().setPacketHandled(true);
    }
}
