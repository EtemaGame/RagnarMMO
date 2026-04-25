package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/**
 * Angelus — Passive/Active (Acolyte)
 * RO: Increases DEF of all party members in range.
 * MC: Applies the ANGELUS MobEffect which increases Armor via AttributeModifiers.
 */
public class AngelusSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "angelus");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        var defOpt = SkillRegistry.get(ID);
        int durationTicks = defOpt
                .map(def -> def.getLevelInt("duration_ticks", level, level * 30 * 20))
                .orElse(level * 30 * 20);
        int amplifier = defOpt
                .map(def -> def.getLevelInt("effect_amplifier", level, level - 1))
                .orElse(level - 1);

        List<ServerPlayer> targets = AcolyteTargetingHelper.collectPartyMembersInRange(player, 16.0);
        for (ServerPlayer target : targets) {
            target.addEffect(new MobEffectInstance(RagnarMobEffects.ANGELUS.get(), durationTicks, amplifier));
            if (player.level() instanceof ServerLevel sl) {
                SkillVisualFx.spawnRing(sl, target.position(), 0.8, 1.2, ParticleTypes.GLOW, 10);
                SkillVisualFx.spawnRing(sl, target.position(), 0.65, 0.15, ParticleTypes.END_ROD, 8);
            }
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.ANGELUS.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
