package com.etema.ragnarmmo.system.skills.network;

import com.etema.ragnarmmo.system.skills.SkillEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUseSkill {
    public final String skillId;

    public PacketUseSkill(String skillId) {
        this.skillId = skillId;
    }

    public static void encode(PacketUseSkill msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId);
    }

    public static PacketUseSkill decode(FriendlyByteBuf buf) {
        return new PacketUseSkill(buf.readUtf());
    }

    public static void handle(PacketUseSkill msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                SkillEffectHandler.tryUseSkill(player, msg.skillId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
