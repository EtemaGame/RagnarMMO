package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FreezingTrapSkillEffect implements ISkillEffect {

    private final HunterTrapManager.TrapDefinition definition;

    public FreezingTrapSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:freezing_trap",
                2.0, // 2 block radius trigger
                20 * 60, // 60 seconds duration
                ParticleTypes.SNOWFLAKE,
                ParticleTypes.SNOWFLAKE,
                (trap, target) -> {
                    // Trigger Logic: 5 seconds of Slowness X + Mining Fatigue
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 9, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 5, 4, false, true));
                });
    }

    @Override
    public void execute(ServerPlayer player, int currentLevel) {
        BlockPos pos = getTargetBlock(player);
        if (pos == null) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_no_target_block"));
            return;
        }

        HunterTrapManager.placeTrap(player, (net.minecraft.server.level.ServerLevel) player.level(), pos.above(),
                definition, currentLevel);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        // Find the block the player is looking at (max distance 6 blocks)
        net.minecraft.world.phys.HitResult hitResult = player.pick(6.0D, 0.0F, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }
}
