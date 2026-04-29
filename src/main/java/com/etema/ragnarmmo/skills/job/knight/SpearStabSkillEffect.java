package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Spear Stab — Active
 * RO: Powerful linear thrust that pushes enemies back (and may push them into the wall for bonus damage).
 *
 * Minecraft:
 *  - Single target hit in front of the player (4.5 block range).
 *  - High damage + strong knockback away from player.
 *  - If target collides with a wall within 1s, bonus damage occurs (approximated by checking if
 *    velocity was zeroed out by the collision — we keep it simple: apply base damage + flat bonus).
 */
public class SpearStabSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "spear_stab");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
