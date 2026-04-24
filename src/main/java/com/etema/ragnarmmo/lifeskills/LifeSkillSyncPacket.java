package com.etema.ragnarmmo.lifeskills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs all life skill data from server to client.
 */
public class LifeSkillSyncPacket {

    private final CompoundTag data;

    public LifeSkillSyncPacket(CompoundTag data) {
        this.data = data;
    }

    public LifeSkillSyncPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleLifeSkillSync(data));
        });
        ctx.get().setPacketHandled(true);
    }

}
