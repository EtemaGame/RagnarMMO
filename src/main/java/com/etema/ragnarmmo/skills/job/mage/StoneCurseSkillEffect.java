package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.entity.effect.StatusOverlayEntity;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Stone Curse — Active (Earth/Status)
 * RO-inspired petrify approximation.
 * Chance now follows the shared table: 24% at Lv1 up to 60% at Lv10.
 * Does not work against Boss or Undead monsters.
 *
 * Minecraft:
 *  - On hit, applies:
 *    - MOVEMENT_SLOWDOWN 10 (completely immobile = Petrify)
 *    - WEAKNESS 2 (represents the defense/attack lockout)
 *    - DAMAGE_RESISTANCE 4 (target becomes nearly invincible while petrified)
 *  - Visual: BLOCK (cobblestone) particles burst from the target + stone-cracking sounds.
 *  - Duration: 2 + level seconds (short but impactful).
 */
public class StoneCurseSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "stone_curse");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        var definition = SkillRegistry.require(ID);
        double range = definition.getLevelDouble("range", level, 12.0D);
        LivingEntity target = MageTargetUtil.raycast(player, range);
        if (target == null) return;

        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD ||
            com.etema.ragnarmmo.mobs.util.MobUtils.isBossLike(target)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_blocked_type")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        float chance = (float) definition.getLevelDouble("success_chance", level,
                0.24D + ((level - 1) * 0.04D));
        if (player.getRandom().nextFloat() > chance) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Stone Curse §cfailed§7."));
            // Still show a small miss particle
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
            }
            return;
        }

        int durationTicks = definition.getLevelInt("duration_ticks", level, (2 + level) * 20);

        // Complete immobility
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 10, false, true, true));
        // Locked attack
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 2, false, true, false));
        // Near-invincibility while petrified (like RO stone status)
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 4, false, false, true));
        if (player.level() instanceof ServerLevel overlayLevel) {
            StatusOverlayEntity.spawnOrRefresh(overlayLevel, target, StatusOverlayEntity.Variant.STONE, durationTicks);
        }

        // Sound: stone cracking
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, 0.8f, 0.5f);

        // Particles: stone block burst
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                    target.getX(), target.getY() + 1, target.getZ(),
                    40, 0.4, 0.5, 0.4, 0.15);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    20, 0.3, 0.3, 0.3, 0.08);
            SkillVisualFx.spawnAuraColumn(sl, target, ParticleTypes.ASH, ParticleTypes.SMOKE, 4, 0.45, target.getBbHeight());
        }
    }
}
