package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class AnkleSnareSkillEffect implements ISkillEffect {

    private final HunterTrapManager.TrapDefinition definition;

    public AnkleSnareSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:ankle_snare",
                1.5, // 1.5 block radius trigger
                20 * 60, // 60 seconds duration
                ParticleTypes.CRIT, // Small visual
                ParticleTypes.ENCHANTED_HIT,
                (trap, target) -> {
                    // Trigger Logic: 5 seconds per level duration
                    int durationTicks = 20 * 5 * trap.skillLevel;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 255, false, true));
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
        net.minecraft.world.phys.HitResult hitResult = player.pick(6.0D, 0.0F, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }
}
