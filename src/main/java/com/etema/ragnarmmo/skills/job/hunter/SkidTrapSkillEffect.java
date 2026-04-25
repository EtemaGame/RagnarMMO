package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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
 * Skid Trap — Active Trap
 * RO: Pushes enemies several cells in a random direction.
 *
 * Minecraft: Launches the triggering entity away from trap + brief levitation.
 */
public class SkidTrapSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "skid_trap");
    private final HunterTrapManager.TrapDefinition definition;

    public SkidTrapSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:skid_trap",
                1.0,
                20 * 120,
                ParticleTypes.CRIT,
                ParticleTypes.SWEEP_ATTACK,
                (trap, target) -> {
                    double dx = target.getX() - trap.position.getX();
                    double dz = target.getZ() - trap.position.getZ();
                    double mag = Math.sqrt(dx * dx + dz * dz);
                    if (mag < 0.01) { dx = 1; dz = 0; mag = 1; }
                    double nx = dx / mag;
                    double nz = dz / mag;

                    double force = 2.0 + (trap.skillLevel * 0.2);
                    target.setDeltaMovement(nx * force, 0.4, nz * force);
                    target.hurtMarked = true;
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 3 + trap.skillLevel, 0, false, false, false));

                    trap.level.playSound(null, trap.position,
                            SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.2f, 0.6f);
                    trap.level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            trap.position.getX() + 0.5, trap.position.getY() + 0.5, trap.position.getZ() + 0.5,
                            8, 0.5, 0.1, 0.5, 0.2);
                });
    }

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        BlockPos pos = getTargetBlock(player);
        if (pos == null) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo block target.")); return; }
        HunterTrapManager.placeTrap(player, (ServerLevel) player.level(), pos.above(), definition, level);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        var h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
