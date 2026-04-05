package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.execution.aoe.GroundAoEPersistentEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.entity.aoe.FireWallAoe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Fire Wall — Active (Ground trap, Fire property)
 * RO: Places a wall of fire in front of the caster.
 * Minecraft: Spawns 3 segments of FireWallAoe perpendicular to player's look.
 */
public class FireWallSkillEffect extends GroundAoEPersistentEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "fire_wall");

    public FireWallSkillEffect() {
        super(ID);
    }

    public FireWallSkillEffect(ResourceLocation id) {
        super(id);
    }

    @Override
    public int getCastTime(int level) {
        // 2s (40) down to 0.65s (13), decrement 0.15s (3) per level
        return 40 - (level - 1) * 3;
    }

    @Override
    protected double getRange(int level) {
        return 2.5; // Wall distance from player
    }

    @Override
    protected void playCastVisuals(LivingEntity user, Vec3 pos, int level) {
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(), 
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void spawnAoE(LivingEntity user, Vec3 pos, int level) {
        if (!(user.level() instanceof ServerLevel sl)) return;

        Vec3 forward = user.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        
        float damage = SkillDamageHelper.scaleByMATK(user, 50.0f);
        int duration = (5 + (level - 1)) * 20; // 5s to 14s
        int maxHits = 3 + (level - 1); // 3 to 12 hits

        // RO Style: 3 segments in a line perpendicular to caster's view
        for (int i = -1; i <= 1; i++) {
            Vec3 segmentPos = pos.add(right.scale(i));
            FireWallAoe aoe = new FireWallAoe(sl, user, 0.7f, damage, duration, maxHits);
            aoe.setPos(segmentPos.x, segmentPos.y, segmentPos.z);
            sl.addFreshEntity(aoe);
        }
    }
}
