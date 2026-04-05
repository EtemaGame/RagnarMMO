package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
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
 * RO: Turns a target to stone (Petrify status), making them immobile but invincible.
 *     Chance of success: (40 + 5×level)%. Doesn't work against Boss monsters.
 *
 * Minecraft:
 *  - On hit, applies:
 *    - MOVEMENT_SLOWDOWN 10 (completely immobile = Petrify)
 *    - WEAKNESS 2 (represents the defense/attack lockout)
 *    - DAMAGE_RESISTANCE 4 (target becomes nearly invincible while petrified)
 *  - Visual: BLOCK (cobblestone) particles burst from the target + stone-cracking sounds.
 *  - Duration: 2 + level seconds (short but impactful).
 *  - Does not work on Bosses (MobTier check would need MobTier data).
 */
public class StoneCurseSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "stone_curse");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = MageTargetUtil.raycast(player, 12.0);
        if (target == null) return;

        // Petrify chance: 40% + 5% per level
        float chance = 0.40f + (0.05f * level);
        if (player.getRandom().nextFloat() > chance) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Stone Curse §cfailed§7."));
            // Still show a small miss particle
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
            }
            return;
        }

        int durationTicks = (2 + level) * 20; // 3s to 12s

        // Complete immobility
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 10, false, true, true));
        // Locked attack
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 2, false, true, false));
        // Near-invincibility while petrified (like RO stone status)
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 4, false, false, true));

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
        }
    }
}
