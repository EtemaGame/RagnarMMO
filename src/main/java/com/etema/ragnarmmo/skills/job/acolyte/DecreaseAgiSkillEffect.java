package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.mobs.util.MobUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Decrease AGI — Active (Acolyte)
 * RO: Reduces target's FLEE and ASPD by lowering their AGI stat.
 * MC: Uses the RO success table, ignores Boss monsters, and applies
 * Slowness / Mining Fatigue as an AGI reduction approximation.
 */
public class DecreaseAgiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "decrease_agi");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = AcolyteTargetingHelper.resolveHostileTarget(player, 8.0);
        if (target == null) {
            player.sendSystemMessage(Component.literal("§cDecrease AGI: §fNo hay objetivo válido."));
            return;
        }

        if (MobUtils.isBossLike(target)) {
            player.sendSystemMessage(Component.literal("§7Decrease AGI no afecta a Boss monsters."));
            return;
        }

        var defOpt = SkillRegistry.get(ID);
        int durationTicks = defOpt
                .map(def -> def.getLevelInt("duration_ticks", level, (20 + (level * 10)) * 20))
                .orElse((20 + (level * 10)) * 20);
        int agiReduction = defOpt
                .map(def -> def.getLevelInt("agi_reduction", level, level + 2))
                .orElse(level + 2);
        float baseChance = defOpt
                .map(def -> def.getLevelDouble("success_rate", level, 40.0 + (level * 2.0)))
                .orElse(40.0 + (level * 2.0))
                .floatValue();
        float intBonus = Math.min(15.0f, SkillDamageHelper.getINT(player) * 0.15f);
        float armorPenalty = (float) Math.min(12.0, (target.getArmorValue() * 0.8) +
                (target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) * 1.5));
        float finalChance = Math.max(5.0f, Math.min(95.0f, baseChance + intBonus - armorPenalty));

        if ((player.getRandom().nextFloat() * 100.0f) > finalChance) {
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(),
                        6, 0.2, 0.3, 0.2, 0.02);
            }
            player.sendSystemMessage(Component.literal("§7Decrease AGI falló sobre "
                    + target.getDisplayName().getString() + "."));
            return;
        }

        int slownessAmplifier = Math.max(0, Math.min(2, (agiReduction - 1) / 4));
        int fatigueAmplifier = agiReduction >= 8 ? 1 : 0;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, slownessAmplifier));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, fatigueAmplifier));

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    10, 0.3, 0.5, 0.3, 0.05);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.8f);
        }

        player.sendSystemMessage(Component.literal("§9↓ Decrease AGI §faplica Slowness a "
                + target.getDisplayName().getString() + " por " + (durationTicks / 20) + "s."));
    }
}
