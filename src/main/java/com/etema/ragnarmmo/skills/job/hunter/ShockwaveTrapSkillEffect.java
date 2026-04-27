package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
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
 * Shockwave Trap — Active Trap
 * RO: Cancels skills (Silence/Stun) for all enemies in a 5x5 area.
 *
 * Minecraft:
 *  - On trigger: applies Nausea (disorientation = Silence proxy) + Weakness to all nearby.
 *  - Nausea disrupts the player's ability to aim/concentrate.
 *  - Wide 3-block trigger radius (5x5 cells in RO).
 */
public class ShockwaveTrapSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "shockwave_trap");
    private final HunterTrapManager.TrapDefinition definition;

    public ShockwaveTrapSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:shockwave_trap",
                2.5,
                20 * 120,
                ParticleTypes.ENCHANTED_HIT,
                ParticleTypes.EXPLOSION,
                (trap, target) -> {
                    AABB area = trap.getBoundingBox();
                    List<LivingEntity> entities = trap.level.getEntitiesOfClass(LivingEntity.class, area,
                            e -> e.isAlive() && e != trap.owner && !e.isAlliedTo(trap.owner));
                    for (LivingEntity entity : entities) {
                        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * (3 + trap.skillLevel), 1, false, true, true));
                        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * (3 + trap.skillLevel), 1, false, true, true));
                        
                        // RO: Drains SP [(15 + 5 * SkillLevel)%]
                        if (entity instanceof ServerPlayer sp) {
                            sp.getCapability(PlayerStatsProvider.CAP).ifPresent(s -> {
                                double drain = s.getMaxResource() * (0.15 + 0.05 * trap.skillLevel);
                                s.consumeResource(drain); // This will handle Mana or SP correctly based on job
                            });
                        } else {
                            // Non-players get blinded as proxy for lack of SP
                            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 5, 0, false, true));
                        }
                    }
                    trap.level.playSound(null, trap.position,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
                    trap.level.sendParticles(ParticleTypes.EXPLOSION,
                            trap.position.getX() + 0.5, trap.position.getY() + 0.5, trap.position.getZ() + 0.5,
                            3, 1.5, 0.5, 1.5, 0.0);
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
                SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
