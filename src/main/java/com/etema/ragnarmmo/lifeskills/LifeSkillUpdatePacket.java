package com.etema.ragnarmmo.lifeskills;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs a single life skill update from server to client.
 * Used for incremental updates (point gains, level ups).
 */
public class LifeSkillUpdatePacket {

    private final LifeSkillType skillType;
    private final int level;
    private final int points;

    public LifeSkillUpdatePacket(LifeSkillType skillType, int level, int points) {
        this.skillType = skillType;
        this.level = level;
        this.points = points;
    }

    public LifeSkillUpdatePacket(FriendlyByteBuf buf) {
        this.skillType = LifeSkillType.fromId(buf.readUtf());
        this.level = buf.readVarInt();
        this.points = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillType != null ? skillType.getId() : "");
        buf.writeVarInt(level);
        buf.writeVarInt(points);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleLifeSkillUpdate(skillType, level,
                            points));
        });
        ctx.get().setPacketHandled(true);
    }

    public LifeSkillType getSkillType() {
        return skillType;
    }

    public int getLevel() {
        return level;
    }

    public int getPoints() {
        return points;
    }
}
