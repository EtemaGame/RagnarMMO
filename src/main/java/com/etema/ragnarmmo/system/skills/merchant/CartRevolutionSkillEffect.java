package com.etema.ragnarmmo.system.skills.merchant;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CartRevolutionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "cart_revolution");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Cart Revolution: Deals heavy physical damage in a 3x3 area around the target
        // and knocks them back.
        // Requires a Pushcart equipped. (We bypass the strict requirement here for
        // placeholder).
        // Damage increases with Cart Weight.

        float baseDamage = 10.0f; // Represents a heavy cart swing
        double radius = 3.0;

        // Introduce a heavy delay for the "cart revolution" swing
        com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(4, () -> {
            boolean hitAny = false;
            AABB area = player.getBoundingBox().inflate(radius);
            List<Entity> nearbyEntities = player.level().getEntities(player, area,
                    e -> e instanceof LivingEntity && e != player && e.isAlive());

            for (Entity e : nearbyEntities) {
                if (e instanceof LivingEntity target && target.isAlive()) {
                    target.hurt(player.damageSources().mobAttack(player), baseDamage);

                    // Knockback logic
                    Vec3 knockbackDir = target.position().subtract(player.position()).normalize();
                    target.knockback(1.5f, -knockbackDir.x, -knockbackDir.z);
                    hitAny = true;
                }
            }

            if (hitAny) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.2f, 0.9f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 10,
                            radius / 2, 0.2, radius / 2, 0.1);
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.1, player.getZ(), 
                            15, radius / 1.5, 0.1, radius / 1.5, 0.05);
                }
            }
        });
    }
}
