package com.etema.ragnarmmo.skill.job.assassin;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.skill.api.ISkillEffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class EnchantPoisonSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:enchant_poison");
    public static final String ENCHANT_POISON_LEVEL_TAG = "ragnarmmo_enchant_poison_level";
    public static final String ENCHANT_POISON_UNTIL_TAG = "ragnarmmo_enchant_poison_until";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int durationTicks = 600 + (level * 200);
        long untilTick = player.level().getGameTime() + durationTicks;

        serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 30,
                0.5, 0.5, 0.5, 0.1);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 0.5f);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 0));
        player.getPersistentData().putInt(ENCHANT_POISON_LEVEL_TAG, level);
        player.getPersistentData().putLong(ENCHANT_POISON_UNTIL_TAG, untilTick);
        CombatPropertyResolver.applyTemporaryWeaponElement(player, ElementType.POISON, untilTick);
        player.sendSystemMessage(Component.literal("Weapon Enchanted: Poison"));
    }
}
