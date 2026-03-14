package com.etema.ragnarmmo.system.skills.hunter;

import com.etema.ragnarmmo.common.api.skills.ISkillDefinition;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
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

                    // Dex + Int scaling for trap damage
                    float trapDamage = 20.0F; // Base
                    if (trap.owner instanceof ServerPlayer player) {
                        trapDamage += player.getCapability(PlayerStatsProvider.CAP)
                                .<Float>map(stats -> (stats.getDEX() * 1.5F) + (stats.getINT() * 0.5F)).orElse(0.0F);
                    }

                    // Apply Fire Damage type if possible, utilizing generic damage source for now
                    DamageSource source = trap.level.damageSources().inFire();

                    for (net.minecraft.world.entity.LivingEntity entity : entities) {
                        entity.hurt(source, trapDamage);
                        entity.setSecondsOnFire(5);
                    }
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
                definition);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult hitResult = player.pick(6.0D, 0.0F, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }
}
