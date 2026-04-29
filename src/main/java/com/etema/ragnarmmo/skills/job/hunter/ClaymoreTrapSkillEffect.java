package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class ClaymoreTrapSkillEffect implements ISkillEffect {

    private final HunterTrapManager.TrapDefinition definition;

    public ClaymoreTrapSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:claymore_trap",
                3.0, // 3 block radius trigger / damage
                20 * 60, // 60 seconds duration
                ParticleTypes.FLAME,
                ParticleTypes.EXPLOSION,
                (trap, target) -> {
                    // Trigger Logic: AoE damage to all entities in range
                    net.minecraft.world.phys.AABB explosiveBounds = trap.getBoundingBox();
                    java.util.List<net.minecraft.world.entity.LivingEntity> entities = trap.level.getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            explosiveBounds,
                            e -> e.isAlive() && e != trap.owner && !e.isAlliedTo(trap.owner));

                    for (net.minecraft.world.entity.LivingEntity entity : entities) {
                        entity.setSecondsOnFire(5);
                    }
                });
    }

    @Override
    public void execute(ServerPlayer player, int currentLevel) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult hitResult = player.pick(6.0D, 0.0F, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }
}
