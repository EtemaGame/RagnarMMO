package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.skill.api.SkillType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSkillXpPacket {
    private final net.minecraft.resources.ResourceLocation skillId;
    private final int amount;

    public ClientboundSkillXpPacket(net.minecraft.resources.ResourceLocation skillId, int amount) {
        this.skillId = skillId;
        this.amount = amount;
    }

    @SuppressWarnings("removal")
    public ClientboundSkillXpPacket(SkillType skillType, int amount) {
        this(skillType != null ? skillType.toResourceLocation() : null, amount);
    }

    public ClientboundSkillXpPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readResourceLocation();
        this.amount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(skillId);
        buf.writeInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleSkillXpGain(skillId, amount)));
        ctx.get().setPacketHandled(true);
    }
}
