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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Sandman — Active Trap (Ghost property)
 * RO: Puts all enemies in a 5×5 area to sleep.
 *
 * Minecraft:
 *  - On trigger: applies extreme SLOWNESS (sleep proxy) + BLINDNESS to all nearby.
 *  - Also applies Nausea briefly for the "dazed waking up" effect.
 */
public class SandmanSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "sandman");
    private final HunterTrapManager.TrapDefinition definition;

    public SandmanSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:sandman",
                2.5,
                20 * 120,
                ParticleTypes.SQUID_INK,
                ParticleTypes.LARGE_SMOKE,
                (trap, target) -> {
                    AABB area = trap.getBoundingBox();
                    List<LivingEntity> entities = trap.level.getEntitiesOfClass(LivingEntity.class, area,
                            e -> e.isAlive() && e != trap.owner && !e.isAlliedTo(trap.owner));

                    for (LivingEntity entity : entities) {
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 10, false, true, true));
                        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 4, 0, false, true, false));
                        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 3, 0, false, false, false));
                    }

                    // Use a known-working SoundEvent for the dreamy atmosphere
                    trap.level.playSound(null, trap.position,
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 0.3f);

                    trap.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                            trap.position.getX() + 0.5, trap.position.getY() + 0.5, trap.position.getZ() + 0.5,
                            40, 2.0, 0.3, 2.0, 0.05);
                    trap.level.sendParticles(ParticleTypes.SQUID_INK,
                            trap.position.getX() + 0.5, trap.position.getY() + 1, trap.position.getZ() + 0.5,
                            20, 1.5, 0.2, 1.5, 0.02);
                });
    }

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        BlockPos pos = getTargetBlock(player);
        if (pos == null) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo block target.")); return; }
        HunterTrapManager.placeTrap(player, (net.minecraft.server.level.ServerLevel) player.level(), pos.above(), definition, level);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        var h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
