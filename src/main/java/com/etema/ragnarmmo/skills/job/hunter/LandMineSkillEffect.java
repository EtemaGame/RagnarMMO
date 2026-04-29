package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "land_mine");
    private final HunterTrapManager.TrapDefinition definition;

    public LandMineSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:land_mine",
                1.2,        // Small point trigger
                20 * 120,   // 2 min duration
                ParticleTypes.FLAME,
                ParticleTypes.EXPLOSION,
                (trap, target) -> {
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
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        net.minecraft.world.phys.HitResult h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
