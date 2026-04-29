package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thunder Storm — Active (Wind/Lightning AoE)
 * Hits a 5x5 area around the targeted spot, with one 80% MATK strike per skill level.
 */
public class ThunderStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "thunder_storm");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public int getCastTime(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_time_ticks", level, Math.max(20, level * 20)))
                .orElse(Math.max(20, level * 20));
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private Vec3 resolveTargetSpot(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 0.0f, false);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        return player.position().add(player.getLookAngle().scale(range));
    }
}
