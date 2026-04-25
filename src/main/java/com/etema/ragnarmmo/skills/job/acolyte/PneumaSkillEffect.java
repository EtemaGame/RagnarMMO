package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.entity.aoe.PneumaAoe;
import com.etema.ragnarmmo.skills.execution.aoe.GroundAoEPersistentEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class PneumaSkillEffect extends GroundAoEPersistentEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "pneuma");

    public PneumaSkillEffect() {
        super(ID);
    }

    @Override
    protected double getRange(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelDouble("range", level, 6.0D))
                .orElse(6.0D);
    }

    @Override
    protected void playCastVisuals(LivingEntity user, Vec3 pos, int level) {
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.25f);
        if (user.level() instanceof ServerLevel serverLevel) {
            SkillVisualFx.spawnRing(serverLevel, pos, 1.25, 0.08, ParticleTypes.END_ROD, 10);
            SkillVisualFx.spawnRing(serverLevel, pos, 0.9, 0.22, ParticleTypes.CLOUD, 8);
        }
    }

    @Override
    protected void spawnAoE(LivingEntity user, Vec3 pos, int level) {
        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = new Vec3(pos.x, Math.floor(pos.y) + 0.05, pos.z);
        var definition = SkillRegistry.require(ID);
        double radius = definition.getLevelDouble("aoe_radius", level, 1.6D);
        int durationTicks = definition.getLevelInt("duration_ticks", level, 200);
        boolean occupied = !serverLevel.getEntitiesOfClass(PneumaAoe.class,
                new net.minecraft.world.phys.AABB(center.x - radius - 0.2D, center.y - 1.0, center.z - radius - 0.2D,
                        center.x + radius + 0.2D, center.y + 1.5, center.z + radius + 0.2D),
                aoe -> aoe.position().distanceToSqr(center) <= radius * radius)
                .isEmpty();
        if (occupied) {
            if (user instanceof net.minecraft.server.level.ServerPlayer player) {
                player.sendSystemMessage(Component.literal("§7Pneuma ya está cubriendo esa zona."));
            }
            return;
        }

        PneumaAoe aoe = new PneumaAoe(serverLevel, user, (float) radius, durationTicks);
        aoe.setPos(center.x, center.y, center.z);
        serverLevel.addFreshEntity(aoe);
    }
}
