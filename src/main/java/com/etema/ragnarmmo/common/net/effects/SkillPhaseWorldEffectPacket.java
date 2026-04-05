package com.etema.ragnarmmo.common.net.effects;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SkillPhaseWorldEffectPacket {
    private final ResourceLocation skillId;
    private final EffectTriggerPhase phase;
    private final Vec3 position;
    private final Vec3 normal;
    private final float scaleMultiplier;
    private final int durationOverrideTicks;

    public SkillPhaseWorldEffectPacket(ResourceLocation skillId, EffectTriggerPhase phase, Vec3 position, Vec3 normal) {
        this(skillId, phase, position, normal, 1.0f, -1);
    }

    public SkillPhaseWorldEffectPacket(ResourceLocation skillId, EffectTriggerPhase phase, Vec3 position, Vec3 normal,
            float scaleMultiplier, int durationOverrideTicks) {
        this.skillId = skillId;
        this.phase = phase;
        this.position = position;
        this.normal = normal;
        this.scaleMultiplier = scaleMultiplier;
        this.durationOverrideTicks = durationOverrideTicks;
    }

    public static void encode(SkillPhaseWorldEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.skillId);
        buf.writeEnum(msg.phase);
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeFloat((float) msg.normal.x);
        buf.writeFloat((float) msg.normal.y);
        buf.writeFloat((float) msg.normal.z);
        buf.writeFloat(msg.scaleMultiplier);
        buf.writeVarInt(msg.durationOverrideTicks);
    }

    public static SkillPhaseWorldEffectPacket decode(FriendlyByteBuf buf) {
        ResourceLocation skillId = buf.readResourceLocation();
        EffectTriggerPhase phase = buf.readEnum(EffectTriggerPhase.class);
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 normal = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float scaleMultiplier = buf.readFloat();
        int durationOverrideTicks = buf.readVarInt();
        return new SkillPhaseWorldEffectPacket(skillId, phase, position, normal, scaleMultiplier,
                durationOverrideTicks);
    }

    public static void handle(SkillPhaseWorldEffectPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandler.handleSkillPhaseWorldEffect(msg.skillId, msg.phase, msg.position, msg.normal,
                        msg.scaleMultiplier, msg.durationOverrideTicks)));
        ctx.setPacketHandled(true);
    }
}
