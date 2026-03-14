package com.etema.ragnarmmo.system.skills.hunter;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Steel Crow — Passive
 * RO: Increases damage against Demi-Human monsters.
 *     In Minecraft: bonus damage against MONSTER category (equivalent to human-like mobs).
 *
 * Uses onOffensiveHurt — any damage source (melee or ranged) gets the bonus.
 * Level 10 = +50% damage against monsters.
 */
public class SteelCrowSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "steel_crow");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = event.getEntity();
        MobCategory cat = target.getType().getCategory();
        // Demi-Human proxy: MONSTER category (zombies, skeletons, players, illagers)
        boolean isDemiHuman = cat == MobCategory.MONSTER;
        if (!isDemiHuman) return;

        float multiplier = 1.0f + (level * 0.05f); // +5%/level up to +50%
        event.setAmount(event.getAmount() * multiplier);

        // Subtle metal-spark visual for the "steel crow" attack
        if (player.level() instanceof ServerLevel sl && player.getRandom().nextFloat() < 0.4f) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    4, 0.2, 0.3, 0.2, 0.06);
        }
    }
}
