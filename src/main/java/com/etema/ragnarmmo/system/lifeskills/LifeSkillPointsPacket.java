package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies client of life skill point gains.
 * Used for HUD display updates.
 */
public class LifeSkillPointsPacket {

    private final LifeSkillType skillType;
    private final int pointsGained;
    private final int currentLevel;
    private final int currentPoints;

    public LifeSkillPointsPacket(LifeSkillType skillType, int pointsGained, int currentLevel, int currentPoints) {
        this.skillType = skillType;
        this.pointsGained = pointsGained;
        this.currentLevel = currentLevel;
        this.currentPoints = currentPoints;
    }

    public LifeSkillPointsPacket(FriendlyByteBuf buf) {
        this.skillType = LifeSkillType.fromId(buf.readUtf());
        this.pointsGained = buf.readVarInt();
        this.currentLevel = buf.readVarInt();
        this.currentPoints = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillType != null ? skillType.getId() : "");
        buf.writeVarInt(pointsGained);
        buf.writeVarInt(currentLevel);
        buf.writeVarInt(currentPoints);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleLifeSkillPointsGain(
                            skillType, pointsGained, currentLevel, currentPoints));
        });
        ctx.get().setPacketHandled(true);
    }

    public LifeSkillType getSkillType() {
        return skillType;
    }

    public int getPointsGained() {
        return pointsGained;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }
}
