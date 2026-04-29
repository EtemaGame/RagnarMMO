package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.job.mage.MageTargetUtil;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/**
 * Jupitel Thunder — Active (Wind/Lightning, single target + knockback)
 * RO: Deals 1+level hits of Wind damage and pushes the target back.
 *
 * Fixed from original: now uses proper raycast targeting (not box inflate),
 * each hit is shown with individual particles, knockback is applied once at end.
 */
public class JupitelThunderSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "jupitel_thunder");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
