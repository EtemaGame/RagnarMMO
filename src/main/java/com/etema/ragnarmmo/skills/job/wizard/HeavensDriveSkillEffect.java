package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Heaven's Drive — Active (Earth AoE)
 * RO: Launches multiple rocks from ground in a 5x5 area.
 *     Deals Earth property MATK damage. Does not work on Undead or Boss monsters.
 *     Damage per hit = MATK × (100 + 40 × level)%.
 *     Hits = 5.
 *
 * Minecraft:
 *  - Erupts stone particles in a ring around the target area.
 *  - Deals Earth-property magic damage to all entities in a 4-block radius.
 *  - Knockback effect pushing entities upward (simulating rocks).
 */
public class HeavensDriveSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "heavens_drive");
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
