package com.etema.ragnarmmo.system.skills.archer;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

/**
 * Arrow Shower — Passive proc on arrow hit
 * RO: AoE damage in 3×3 cell area centred on the arrow's target.
 *
 * Added: particles at each hit entity + uses damageSources.arrow() for proper damage type.
 */
public class ArrowShowerSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "arrow_shower");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.getOwner() != player) return;

        double radius = 2.5 + (level * 0.25); // 2.75 to 5 blocks

        LivingEntity primary = event.getEntity();
        Vec3 pos = primary.position();

        AABB area = new AABB(pos.x - radius, pos.y - 2, pos.z - radius,
                              pos.x + radius, pos.y + 2, pos.z + radius);
        List<Entity> nearby = primary.level().getEntities(primary, area,
                e -> e instanceof LivingEntity && e != player && e != primary);

        // Splash = 50% + 5%/level of the original hit damage
        float splashDamage = event.getAmount() * (0.5f + level * 0.05f);

        for (Entity e : nearby) {
            LivingEntity victim = (LivingEntity) e;
            victim.hurt(player.damageSources().arrow(arrow, player), splashDamage);

            // Arrow rain particles at each hit entity
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        victim.getX(), victim.getY() + 1, victim.getZ(),
                        6, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Show the rain of arrows effect at the impact site
        if (player.level() instanceof ServerLevel sl && !nearby.isEmpty()) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y + 1, pos.z, 4, radius / 3, 0.2, radius / 3, 0.0);
        }
    }
}
