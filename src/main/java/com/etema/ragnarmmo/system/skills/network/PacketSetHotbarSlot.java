package com.etema.ragnarmmo.system.skills.network;

import com.etema.ragnarmmo.system.skills.PlayerSkillsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetHotbarSlot {
    public final int slot;
    public final String skillId;

    public PacketSetHotbarSlot(int slot, String skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public static void encode(PacketSetHotbarSlot msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
        buf.writeUtf(msg.skillId);
    }

    public static PacketSetHotbarSlot decode(FriendlyByteBuf buf) {
        return new PacketSetHotbarSlot(buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketSetHotbarSlot msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                PlayerSkillsProvider.get(player).ifPresent(skills -> {
                    skills.setHotbarSlot(msg.slot, msg.skillId);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
