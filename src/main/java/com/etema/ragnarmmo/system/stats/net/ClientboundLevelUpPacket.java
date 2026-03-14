package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.common.api.skills.SkillType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundLevelUpPacket {
    private final ResourceLocation skillId;
    private final int newLevel;

    public ClientboundLevelUpPacket(ResourceLocation skillId, int newLevel) {
        this.skillId = skillId;
        this.newLevel = newLevel;
    }

    @SuppressWarnings("removal")
    public ClientboundLevelUpPacket(SkillType skillType, int newLevel) {
        this(skillType != null ? skillType.toResourceLocation() : null, newLevel);
    }

    public ClientboundLevelUpPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readResourceLocation();
        this.newLevel = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(skillId);
        buf.writeInt(newLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleSkillLevelUp(skillId, newLevel)));
        ctx.get().setPacketHandled(true);
    }
}
