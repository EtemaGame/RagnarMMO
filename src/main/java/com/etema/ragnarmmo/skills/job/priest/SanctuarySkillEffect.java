package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.skills.execution.aoe.GroundAoEPersistentEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.entity.aoe.SanctuaryAoe;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Sanctuary — Area recovery/holy damage.
 * Heals allies and damages Undead/Demons in a persistent area.
 */
public class SanctuarySkillEffect extends GroundAoEPersistentEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sanctuary");

    public SanctuarySkillEffect() {
        super(ID);
    }

    public SanctuarySkillEffect(ResourceLocation id) {
        super(id);
    }

    @Override
    protected double getRange(int level) {
        return 7.0;
    }

    @Override
    protected void playCastVisuals(LivingEntity user, Vec3 pos, int level) {
        user.level().playSound(null, pos.x, pos.y, pos.z, 
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (user.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 40, 4.0, 0.1, 4.0, 0.05);
        }
    }

    @Override
    protected void spawnAoE(LivingEntity user, Vec3 pos, int level) {
        if (!(user.level() instanceof ServerLevel sl)) return;

        int duration = (4 + level) * 20; // roughly level + 4 seconds
        float healAmount = 2.0f + (level * 0.5f);
        float radius = 4.0f;

        SanctuaryAoe aoe = new SanctuaryAoe(sl, user, radius, 0.0f, healAmount, duration);
        aoe.setPos(pos.x, pos.y, pos.z);
        sl.addFreshEntity(aoe);
    }
}
