package com.etema.ragnarmmo.common.net.effects;

import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public final class SkillEffectsNetwork {
    private SkillEffectsNetwork() {
    }

    public static void register(SimpleChannel channel, AtomicInteger id) {
        channel.messageBuilder(SkillPhaseWorldEffectPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillPhaseWorldEffectPacket::encode)
                .decoder(SkillPhaseWorldEffectPacket::decode)
                .consumerMainThread(SkillPhaseWorldEffectPacket::handle)
                .add();
    }

    public static void sendImpact(Entity source, ResourceLocation skillId, HitResult result) {
        if (source == null || source.level().isClientSide || skillId == null || result == null) {
            return;
        }

        Vec3 normal = switch (result.getType()) {
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) result;
                yield Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
            }
            case ENTITY -> {
                Vec3 deltaMovement = source.getDeltaMovement();
                if (deltaMovement.lengthSqr() < 1.0E-6) {
                    yield new Vec3(0.0, 1.0, 0.0);
                }
                yield deltaMovement.normalize().scale(-1.0);
            }
            default -> new Vec3(0.0, 1.0, 0.0);
        };

        Network.sendTrackingEntityAndSelf(source,
                new SkillPhaseWorldEffectPacket(skillId, EffectTriggerPhase.IMPACT, result.getLocation(), normal));
    }
}
