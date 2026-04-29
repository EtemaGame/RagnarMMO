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

import java.util.List;

/**
 * Brandish Spear — Active (Wide AoE swing)
 * RO: Swings the spear in a 3×3 area pattern up to 7×7 at high levels.
 *     Best used while mounted (Peco Peco) for a bonus radius.
 *     Damage = (Spear ATK) × (100 + 40×level)%
 *
 * Minecraft:
 *  - AoE around the player in a frontal cone (5 blocks base radius).
 *  - Deals damage to all entities in range.
 *  - If the player is riding a mount, radius is doubled (Peco Peco equivalent).
 *  - Sweeping arc particles radiating from the player center.
 */
public class BrandishSpearSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "brandish_spear");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
