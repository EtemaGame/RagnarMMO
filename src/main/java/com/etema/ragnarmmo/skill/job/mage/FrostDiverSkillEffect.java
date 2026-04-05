package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.entity.effect.StatusOverlayEntity;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Frost Diver — Active (Water property)
 * Attacks a target with an ice spell that has the chance of freezing it.
 * Frozen targets become Water property and take increased Wind damage (175%).
 * Does not work on Boss or Undead monsters.
 */
public class FrostDiverSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "frost_diver");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getCastTime(int level) {
        return 16; // 0.8s
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = MageTargetUtil.raycast(player, 12.0);
        if (target == null) return;

        // Restriction: Boss and Undead monsters
        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD || 
            com.etema.ragnarmmo.system.mobstats.util.MobUtils.isMVPBoss(target)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_blocked_type")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        // Damage: 110% + 10% per level above 1
        float matkPercent = 110.0f + (level - 1) * 10.0f;
        float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, matkPercent);
        
        boolean hit = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, 
                player.level().damageSources().magic(), damage);

        if (hit) {
            // Freezing Chance: 38% + 3% per level above 1
            float freezeChance = 0.38f + (level - 1) * 0.03f;
            if (player.getRandom().nextFloat() <= freezeChance) {
                // Duration: 3s + 3s per level
                int durationTicks = level * 3 * 20;
                target.addEffect(new MobEffectInstance(com.etema.ragnarmmo.common.init.RagnarMobEffects.FROZEN.get(), durationTicks));
                
                // Visual freeze overlay/ticks
                target.setTicksFrozen(durationTicks);
                if (player.level() instanceof ServerLevel frozenLevel) {
                    StatusOverlayEntity.spawnOrRefresh(frozenLevel, target, StatusOverlayEntity.Variant.FROZEN, durationTicks);
                    SkillVisualFx.spawnBlockBurst(frozenLevel, target, Blocks.BLUE_ICE.defaultBlockState(), 26, 0.35, 0.55, 0.04);
                    frozenLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                            target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.22, 0.45, 0.22, 0.02);
                }
                
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.6f);
            }
        }

        // Skill Visuals
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            SkillVisualFx.spawnAuraColumn(sl, target, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, 4, 0.55, target.getBbHeight());
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.SNOW_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }
}
