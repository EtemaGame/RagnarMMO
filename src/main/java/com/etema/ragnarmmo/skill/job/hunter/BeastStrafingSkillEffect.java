package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.execution.projectile.RagnarArrowSpawnHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Beast Strafing — Passive
 * RO: Fires two additional arrows when attacking a Brute monster.
 *     In higher RO versions: rapid-fires against beast/insect type enemies.
 *
 * Minecraft:
 *  - onOffensiveHurt: when an arrow hits a CREATURE or Arthropod mob,
 *    spawns 2 additional arrows at the target with slight spread.
 *  - Level scales arrow damage bonus per extra arrow.
 */
public class BeastStrafingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "beast_strafing");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;

        LivingEntity target = event.getEntity();
        MobCategory cat = target.getType().getCategory();
        boolean isBeast = cat == MobCategory.CREATURE || cat == MobCategory.WATER_CREATURE
                || target.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
        if (!isBeast) return;

        float bonusDmg = event.getAmount() * (0.2f + level * 0.05f);
        int extraArrows = Math.min(level / 3 + 1, 3); // 1 to 3 extra arrows

        for (int i = 0; i < extraArrows; i++) {
            double spread = 0.08 * (i + 1);
            Vec3 look = player.getLookAngle();
            Vec3 direction = new Vec3(
                    look.x + player.getRandom().nextGaussian() * spread,
                    look.y + player.getRandom().nextGaussian() * spread,
                    look.z + player.getRandom().nextGaussian() * spread);
            RagnarArrowSpawnHelper.spawn(player, direction, 2.5F, 0.0F, 1.0F,
                    arrow -> {
                        arrow.setBaseDamage(bonusDmg);
                        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }, null);
        }

        // Crit particles at target to indicate the bonus shots
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    6 * extraArrows, 0.3, 0.3, 0.3, 0.08);
        }
    }
}
