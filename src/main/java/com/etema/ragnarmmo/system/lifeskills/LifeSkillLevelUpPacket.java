package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies client of life skill level up.
 */
public class LifeSkillLevelUpPacket {

    private final LifeSkillType skillType;
    private final int newLevel;

    public LifeSkillLevelUpPacket(LifeSkillType skillType, int newLevel) {
        this.skillType = skillType;
        this.newLevel = newLevel;
    }

    public LifeSkillLevelUpPacket(FriendlyByteBuf buf) {
        this.skillType = LifeSkillType.fromId(buf.readUtf());
        this.newLevel = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillType != null ? skillType.getId() : "");
        buf.writeVarInt(newLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleLifeSkillLevelUp(skillType,
                            newLevel));
        });
        ctx.get().setPacketHandled(true);
    }

    public LifeSkillType getSkillType() {
        return skillType;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
