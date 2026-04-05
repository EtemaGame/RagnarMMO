package com.etema.ragnarmmo.skill.net;

import com.etema.ragnarmmo.skill.runtime.SkillEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUseSkill {
    private final String skillId;

    public PacketUseSkill(String skillId) {
        this.skillId = skillId;
    }

    public PacketUseSkill(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SkillEffectHandler.tryUseSkill(player, skillId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
