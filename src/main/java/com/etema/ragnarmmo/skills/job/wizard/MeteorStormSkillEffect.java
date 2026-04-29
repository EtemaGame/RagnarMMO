package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Meteor Storm — Active (Multi-hit Fire Rain)
 * RO: Drops 2×Level meteors in a random area around the caster.
 *     Each hit deals MATK×(100+40×Level)% and has a 30% chance to burn.
 *
 * Minecraft:
 *  - Fires Level/2 waves of delayed AoE damage with lava/explosion particles.
 *  - Each wave performs all hits at once on nearby entities.
 */
public class MeteorStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "meteor_storm");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
