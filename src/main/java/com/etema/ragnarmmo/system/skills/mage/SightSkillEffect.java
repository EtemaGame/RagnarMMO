package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Sight — Active
 * RO: Reveals all hidden/cloaked enemies in a 3×3 area for 10 seconds.
 *     Breaks Hiding and Cloaking status on nearby enemies.
 *
 * Minecraft:
 *  - Reveals all nearby entities by applying GLOWING + sending their positions to the player.
 *  - Sends a list of visible entity names via chat.
 *  - Works as a counter to Cloak (Assassin skill that sets invisible flag).
 *  - Breaks `ragnar_cloaked` PersistentData flag on mobs.
 */
public class SightSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sight");
    public static final String CLOAKED_TAG = "ragnar_cloaked_until";

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double radius = 5.0 + level * 0.5;
        int durationTicks = 200; // 10 seconds

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        int revealed = 0;
        for (LivingEntity target : targets) {
            // Remove cloak if present
            if (target.getPersistentData().contains(CLOAKED_TAG)) {
                target.getPersistentData().remove(CLOAKED_TAG);
                target.setInvisible(false);
            }
            // Apply Glowing — makes entity visible through walls for all nearby players
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, false, false, false));
            revealed++;
        }

        // Sound & particles: magical pulse emanating outward
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.3f);

        if (player.level() instanceof ServerLevel sl) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                double px = player.getX() + Math.cos(angle) * radius;
                double pz = player.getZ() + Math.sin(angle) * radius;
                sl.sendParticles(ParticleTypes.ENCHANT, px, player.getY() + 1, pz, 3, 0.1, 0.2, 0.1, 0.02);
            }
            sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        }

        if (revealed > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eSight: §f" + revealed + " entit" + (revealed == 1 ? "y" : "ies") + " revealed."));
        }
    }
}
