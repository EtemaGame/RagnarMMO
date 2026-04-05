package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillManager;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

/**
 * Blitz Beat — Active / Passive Proc
 * RO: Falcon attacks a target and nearby enemies (AoE splash).
 * 
 * Active: Command Falcon to strike.
 * Passive (Auto-Blitz): Procs on normal hits with a bow, based on LUK.
 * 
 * Damage Formula (RO-inspired):
 * DMG = [80 + 20*SteelCrowLvl + 2*(DEX/10 + INT)] per hit.
 * Number of hits = 1 + (SkillLevel-1)/2  (Max 5 hits at Level 10).
 */
public class BlitzBeatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "blitz_beat");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0 || !hasFalcon(player)) return;

        // Manual targeting logic as getTargetEntity is undefined
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 viewVec = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 reachVec = eyePos.add(viewVec.x * 15.0, viewVec.y * 15.0, viewVec.z * 15.0);
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(15.0)).inflate(1.0, 1.0, 1.0);
        
        LivingEntity target = null;
        double minDist = 15.0 * 15.0;
        
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != player && entity.isAlive())) {
            net.minecraft.world.phys.AABB aabb = e.getBoundingBox().inflate(e.getPickRadius());
            java.util.Optional<net.minecraft.world.phys.Vec3> clip = aabb.clip(eyePos, reachVec);
            if (clip.isPresent()) {
                double dist = eyePos.distanceToSqr(clip.get());
                if (dist < minDist) {
                    target = e;
                    minDist = dist;
                }
            }
        }
        
        if (target == null) return;

        performBlitz(player, target, level, false);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0 || !hasFalcon(player)) return;
        
        // Auto-Blitz only procs on ranged (arrow) hits in RO
        if (!(event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow)) return;

        // Auto-Blitz Chance: (LUK * 0.3 + 1)%
        double luk = player.getCapability(PlayerStatsProvider.CAP)
                .map(s -> (double) s.getLUK()).orElse(0.0);
        
        double chance = (luk * 0.3 + 1.0) / 100.0;
        if (player.getRandom().nextDouble() < chance) {
            performBlitz(player, event.getEntity(), level, true);
        }
    }

    private void performBlitz(ServerPlayer player, LivingEntity primary, int level, boolean isAuto) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // RO: Hits = 1 + (Level-1)/2
        int hits = 1 + (level - 1) / 2;
        
        // --- Calculate Base Damage per hit ---
        // RO formula: [80 + 20*SteelCrowLvl + 2*(DEX/10 + INT)]
        // We retrieve stats and Steel Crow level
        double dex = player.getCapability(PlayerStatsProvider.CAP)
                .map(s -> (double) s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.DEX)).orElse(0.0);
        double intel = player.getCapability(PlayerStatsProvider.CAP)
                .map(s -> (double) s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.INT)).orElse(0.0);
        
        // Retrieve Steel Crow level
        int steelCrowLvl = PlayerSkillsProvider.get(player)
                .map(sm -> sm.getSkillLevel(new ResourceLocation("ragnarmmo", "steel_crow")))
                .orElse(0);
        
        // Steel Crow buff: +6 per level (RO standard for Job 50 equivalent)
        float baseDmg = (float) (80 + 6 * steelCrowLvl + 2 * (dex / 10.0 + intel));
        
        // Split damage into multiple hits via SkillSequencer to bypass i-frames
        // Split damage into multiple hits via SkillSequencer to bypass i-frames
        for (int i = 0; i < hits; i++) {
            final int hitNum = i;
            SkillSequencer.schedule(i * 2, () -> {
                if (!primary.isAlive()) return;
                
                // Blitz Beat is Neutral property but ignores DEF in RO
                SkillDamageHelper.dealSkillDamage(primary, player.damageSources().playerAttack(player), baseDmg);
                
                // Falcon sound/particles at target
                serverLevel.playSound(null, primary.getX(), primary.getY(), primary.getZ(),
                        SoundEvents.PARROT_FLY, SoundSource.PLAYERS, 1.0f, 1.5f);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        primary.getX(), primary.getY() + 1, primary.getZ(),
                        1, 0.1, 0.1, 0.1, 0.05);

                // Splash AoE damage
                double splashRadius = 2.0;
                AABB splashArea = primary.getBoundingBox().inflate(splashRadius);
                List<LivingEntity> splashTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, splashArea,
                        e -> e != player && e != primary && e.isAlive());
                
                float splashDmg = isAuto ? (baseDmg / Math.max(1, splashTargets.size() + 1)) : baseDmg;
                for (LivingEntity s : splashTargets) {
                    SkillDamageHelper.dealSkillDamage(s, player.damageSources().playerAttack(player), splashDmg);
                }
            });
        }

        // Global Falcon sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    private boolean hasFalcon(ServerPlayer player) {
        return player.getPersistentData().getBoolean("ragnar_has_falcon");
    }
}
