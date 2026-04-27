package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.skills.execution.aoe.GroundAoEPersistentEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.entity.aoe.FireWallAoe;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_wall");

    public FireWallSkillEffect() {
        super(ID);
    }

    public FireWallSkillEffect(ResourceLocation id) {
        super(id);
    }

    @Override
    public int getCastTime(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_time_ticks", level, 40 - (level - 1) * 3))
                .orElse(40 - (level - 1) * 3);
    }

    @Override
    protected double getRange(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelDouble("range", level, 2.5D))
                .orElse(2.5D);
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
        
        var definition = SkillRegistry.require(ID);
        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 50.0D);
        float damage = SkillDamageHelper.scaleByMATK(user, damagePercent);
        int duration = definition.getLevelInt("duration_ticks", level, (5 + (level - 1)) * 20);
        int maxHits = definition.getLevelInt("max_hits", level, 3 + (level - 1));
        int segmentCount = Math.max(1, definition.getLevelInt("segment_count", level, 3));

        // RO Style: 3 segments in a line perpendicular to caster's view
        int left = -(segmentCount / 2);
        int rightLimit = left + segmentCount;
        for (int i = left; i < rightLimit; i++) {
            Vec3 segmentPos = pos.add(right.scale(i));
            FireWallAoe aoe = new FireWallAoe(sl, user, 0.7f, damage, duration, maxHits);
            aoe.setPos(segmentPos.x, segmentPos.y, segmentPos.z);
            sl.addFreshEntity(aoe);
        }
    }
}
