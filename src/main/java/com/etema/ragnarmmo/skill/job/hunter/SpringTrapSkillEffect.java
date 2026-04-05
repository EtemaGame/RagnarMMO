package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Spring Trap — Active Trap
 * RO: Launches entities upward and removes CC effects.
 */
public class SpringTrapSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "spring_trap");
    private final HunterTrapManager.TrapDefinition definition;

    public SpringTrapSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:spring_trap",
                1.0,
                20 * 120,
                ParticleTypes.END_ROD,
                ParticleTypes.CRIT,
                (trap, target) -> {
                    double launch = 1.5 + (trap.skillLevel * 0.2);
                    target.setDeltaMovement(target.getDeltaMovement().add(0, launch, 0));
                    target.hurtMarked = true;
                    target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                    trap.level.playSound(null, trap.position,
                            SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, 2.0f);
                    trap.level.sendParticles(ParticleTypes.CRIT,
                            trap.position.getX() + 0.5, trap.position.getY() + 1, trap.position.getZ() + 0.5,
                            15, 0.3, 0.5, 0.3, 0.1);
                });
    }

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        BlockPos pos = getTargetBlock(player);
        if (pos == null) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo block target.")); return; }
        HunterTrapManager.placeTrap(player, (ServerLevel) player.level(), pos.above(), definition, level);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        var h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
