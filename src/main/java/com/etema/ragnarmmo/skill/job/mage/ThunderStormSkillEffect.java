package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thunder Storm — Active (Wind/Lightning AoE)
 * Hits a 5x5 area around the targeted spot, with one 80% MATK strike per skill level.
 */
public class ThunderStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "thunder_storm");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public int getCastTime(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_time_ticks", level, Math.max(20, level * 20)))
                .orElse(Math.max(20, level * 20));
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        var definition = SkillRegistry.require(ID);
        double radius = definition.getLevelDouble("aoe_radius", level, 2.5D);
        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 80.0D);
        float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, damagePercent);
        Vec3 center = resolveTargetSpot(player, definition.getLevelDouble("range", level, 15.0D));

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);

            int strikeCount = definition.getLevelInt("hit_count", level, Math.min(level, 10));
            int hitSpacingTicks = definition.getLevelInt("hit_spacing_ticks", level, 5);
            for (int i = 0; i < strikeCount; i++) {
                int delay = i * hitSpacingTicks;
                
                com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(delay, () -> {
                    double ox = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                    double oz = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                    Vec3 strikePos = center.add(ox, 0, oz);
                    Vec3 startPos = strikePos.add(0, 10, 0);

                    com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile projectile = 
                        new com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile(player.level(), player, damage);
                    
                    projectile.setPos(startPos.x, startPos.y, startPos.z);
                    projectile.shoot(0, -1, 0, 1.5f, 0.0f);
                    
                    player.level().addFreshEntity(projectile);
                    
                    // Flash effect for each strike
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FLASH, strikePos.x, strikePos.y + 0.1, strikePos.z, 1, 0, 0, 0, 0);
                    }
                });
            }
        }
    }

    private Vec3 resolveTargetSpot(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 0.0f, false);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        return player.position().add(player.getLookAngle().scale(range));
    }
}
