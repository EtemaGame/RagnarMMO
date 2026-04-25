package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Blast Mine — Active Trap (Wind/Lightning)
 * RO: Deals Wind-property damage in a 5×5 area when triggered.
 *     Damage: 250 + DEX × level. Strong AoE knockback.
 */
public class BlastMineSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "blast_mine");
    private final HunterTrapManager.TrapDefinition definition;

    public BlastMineSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:blast_mine",
                2.0,
                20 * 120,
                ParticleTypes.ELECTRIC_SPARK,
                ParticleTypes.FLASH,
                (trap, target) -> {
                    // AoE: affect all entities in blast radius
                    AABB blastArea = trap.getBoundingBox().inflate(1.5);
                    List<LivingEntity> entities = trap.level.getEntitiesOfClass(LivingEntity.class, blastArea,
                            e -> e.isAlive() && e != trap.owner && !e.isAlliedTo(trap.owner));

                    float baseDamage = 0.0f;
                    if (trap.owner instanceof ServerPlayer player) {
                        double dex = player.getCapability(PlayerStatsProvider.CAP)
                                .map(s -> (double) s.getDEX()).orElse(0.0);
                        double intel = player.getCapability(PlayerStatsProvider.CAP)
                                .map(s -> (double) s.getINT()).orElse(0.0);
                        
                        // RO: [DEX * (3 + 4 * INT / 100) * (SkillLevel / 5)]
                        baseDamage = (float) (dex * (3 + 4 * intel / 100.0) * (trap.skillLevel / 5.0));
                    }
                    if (baseDamage < 10) baseDamage = 10;

                    for (LivingEntity entity : entities) {
                        entity.hurt(trap.level.damageSources().magic(), baseDamage);
                        entity.knockback(1.5,
                                entity.getX() - trap.position.getX(),
                                entity.getZ() - trap.position.getZ());
                    }

                    // Thunder + explosion sound + lightning spark columns
                    trap.level.playSound(null, trap.position,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.2f);
                    trap.level.playSound(null, trap.position,
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.5f);

                    ServerLevel sl2 = trap.level;
                    sl2.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            trap.position.getX() + 0.5, trap.position.getY() + 0.5, trap.position.getZ() + 0.5,
                            80, 1.5, 0.5, 1.5, 0.2);
                    sl2.sendParticles(ParticleTypes.FLASH,
                            trap.position.getX() + 0.5, trap.position.getY() + 1, trap.position.getZ() + 0.5,
                            3, 0.3, 0.3, 0.3, 0);
                });
    }

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        BlockPos pos = getTargetBlock(player);
        if (pos == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo block target found."));
            return;
        }
        HunterTrapManager.placeTrap(player, (ServerLevel) player.level(), pos.above(), definition, level);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
