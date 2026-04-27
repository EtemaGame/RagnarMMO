package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class HeavensDriveAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "heavens_drive");

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    public HeavensDriveAoe(EntityType<? extends HeavensDriveAoe> type, Level level) {
        super(type, level);
    }

    public HeavensDriveAoe(Level level, LivingEntity owner, float radius, float damage, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.HEAVENS_DRIVE_AOE.get(), level, owner, radius, damage, duration);
    }

    @Override
    public void applyEffect(LivingEntity target) {
        SkillDamageHelper.dealSkillDamage(target, this.damageSources().magic(), this.damage);
        target.push(0, 0.1, 0);
    }

    @Override
    public void ambientParticles() {
        if (tickCount % 5 == 0 && level() instanceof ServerLevel sl) {
            float r = getRadius();
            for (int i = 0; i < 8; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * r;
                double px = getX() + Math.cos(angle) * dist;
                double pz = getZ() + Math.sin(angle) * dist;
                
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 
                        px, getY(), pz, 10, 0.1, 0.5, 0.1, 0.1);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, px, getY(), pz, 2, 0.1, 0.2, 0.1, 0.02);
            }
            
            level().playSound(null, getX(), getY(), getZ(), 
                    SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}
