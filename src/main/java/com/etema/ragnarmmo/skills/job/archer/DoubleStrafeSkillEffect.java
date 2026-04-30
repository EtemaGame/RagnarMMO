package com.etema.ragnarmmo.skills.job.archer;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.execution.projectile.ProjectileSkillHelper;
import com.etema.ragnarmmo.skills.execution.projectile.RagnarArrowSpawnHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import com.etema.ragnarmmo.items.runtime.RangedWeaponStatsHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Double Strafe - bow-only two-hit ranged skill.
 */
public class DoubleStrafeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "double_strafe");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
