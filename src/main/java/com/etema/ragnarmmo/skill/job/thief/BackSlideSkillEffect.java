package com.etema.ragnarmmo.skill.job.thief;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class BackSlideSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "back_slide");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Back Slide: Immediately slides the player backward exactly 5 cells (blocks).

        Vec3 look = player.getLookAngle();
        // Nullify Y to slide straight back horizontally
        Vec3 backVec = new Vec3(-look.x, 0.2, -look.z).normalize().scale(1.5);

        // Add momentum. In actual gameplay, setDeltaMovement handles this.
        player.setDeltaMovement(backVec.x, backVec.y, backVec.z);
        player.hurtMarked = true; // Ensure client syncs motion

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.HORSE_SADDLE, SoundSource.PLAYERS, 1.0f, 1.5f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.4,
                    0.1, 0.4, 0.1);
        }
    }
}
