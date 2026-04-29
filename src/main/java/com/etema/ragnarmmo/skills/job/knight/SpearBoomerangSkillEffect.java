package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Spear Boomerang — Active (Ranged)
 * RO: Throws the spear at the target and it returns. Long range.
 *     Damage = (Spear ATK) × (150 + 50×level)%
 *
 * Minecraft:
 *  - Raycasts forward up to 9 blocks to find a target.
 *  - Deals ranged physical damage.
 *  - Visual: particle trail simulating the thrown spear going and returning.
 *  - Does NOT actually spawn a trident entity — a real ThrownTrident
 *    would remove the item; instead we simulate it as a hitscan with effects.
 */
public class SpearBoomerangSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "spear_boomerang");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private LivingEntity raycastTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = new AABB(start, end).inflate(0.5);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(0.3).clip(start, end).isPresent())
                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}
