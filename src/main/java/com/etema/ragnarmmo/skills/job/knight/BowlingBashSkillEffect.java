package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bowling Bash — Active (Chain AoE knockback)
 * RO: High damage + knockback. Enemies hit by the primary target can trigger
 *     secondary hits as the target "bowls" into adjacent enemies.
 *     Damage = 400% + 100%×level ATK.
 *
 * Minecraft:
 *  - Primary: Heavy hit on closest target in front (4 block range), sending them flying.
 *  - Secondary: If the primary target's knockback trajectory hits another mob within 3 blocks,
 *    that mob also takes damage (chain effect).
 *  - Particle cone radiating forward to illustrate the "bowling" motion.
 */
public class BowlingBashSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "bowling_bash");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
