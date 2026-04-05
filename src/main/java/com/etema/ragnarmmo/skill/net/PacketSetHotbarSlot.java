package com.etema.ragnarmmo.skill.net;

import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skill.runtime.SkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetHotbarSlot {
    private final int slot;
    private final String skillId;

    public PacketSetHotbarSlot(int slot, String skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public PacketSetHotbarSlot(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.skillId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeUtf(skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PlayerSkillsProvider.get(player).ifPresent(manager -> {
                    manager.setHotbarSlot(slot, skillId);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
