package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies client that a perk choice is available.
 */
public class LifeSkillPerkChoicePacket {

    private final LifeSkillType skillType;
    private final int tier;

    public LifeSkillPerkChoicePacket(LifeSkillType skillType, int tier) {
        this.skillType = skillType;
        this.tier = tier;
    }

    public LifeSkillPerkChoicePacket(FriendlyByteBuf buf) {
        this.skillType = LifeSkillType.fromId(buf.readUtf());
        this.tier = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillType != null ? skillType.getId() : "");
        buf.writeVarInt(tier);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleLifeSkillPerkChoice(skillType,
                            tier));
        });
        ctx.get().setPacketHandled(true);
    }

    public LifeSkillType getSkillType() {
        return skillType;
    }

    public int getTier() {
        return tier;
    }
}
