package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Land Mine — Active Trap (Fire property)
 * RO: Sets a fire-element trap that deals ATK% × level damage to the
 *     first enemy that steps on it. Single-hit, small radius.
 */
public class LandMineSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "land_mine");
    private final HunterTrapManager.TrapDefinition definition;

    public LandMineSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:land_mine",
                1.2,        // Small point trigger
                20 * 120,   // 2 min duration
                ParticleTypes.FLAME,
                ParticleTypes.EXPLOSION,
                (trap, target) -> {
                    float damage = 0.0f;
                    if (trap.owner instanceof ServerPlayer player) {
                        double dex = player.getCapability(PlayerStatsProvider.CAP)
                                .map(s -> (double) s.getDEX()).orElse(0.0);
                        double intel = player.getCapability(PlayerStatsProvider.CAP)
                                .map(s -> (double) s.getINT()).orElse(0.0);
                        
                        // RO: [DEX * (3 + 4 * INT / 100) * (SkillLevel / 5)]
                        damage = (float) (dex * (3 + 4 * intel / 100.0) * (trap.skillLevel / 5.0));
                    }
                    if (damage < 10) damage = 10;
                    target.hurt(trap.level.damageSources().explosion(null), damage);
                    target.setSecondsOnFire(4);
                    // Explosion sound at trap site
                    trap.level.playSound(null, target.blockPosition(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.5f);
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
        HunterTrapManager.placeTrap(player, (net.minecraft.server.level.ServerLevel) player.level(), pos.above(), definition, level);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
