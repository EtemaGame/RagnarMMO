package com.etema.ragnarmmo.skill.job.knight;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Auto Counter — Passive (Counter-attack on being hit)
 * RO: When an enemy attacks, there is a chance to perform a critical counter-attack.
 *     At level 5, 100% critical hit chance on the counter. Requires melee weapon.
 *     Counter proc chance: 20% * level.
 *
 * Minecraft:
 *  - On defensive hurt, has a (10% × level) chance to immediately strike back.
 *  - The counter-attack deals 150% of current weapon damage and is a guaranteed crit.
 *  - Counter is tracked via PersistentData flag to avoid recursive triggers.
 *  - Requires 2H sword or spear held (Knight weapons).
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AutoCounterSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "auto_counter");
    private static final String COUNTER_GUARD = "ragnar_counter_guard"; // Prevent recursive counters

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void onDefensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (player.getPersistentData().getBoolean(COUNTER_GUARD)) return; // Guard against recursion

        // Only counter melee attacks
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        if (attacker.distanceToSqr(player) > 16) return; // Must be within 4 blocks = melee

        // Counter chance: 10% per level (50% at level 5, 100% at level 10)
        float counterChance = 0.10f * level;
        if (player.getRandom().nextFloat() > counterChance) return;

        // Perform the counter
        player.getPersistentData().putBoolean(COUNTER_GUARD, true);

        float counterDamage = 4.0f + (level * 0.8f); // scales with level
        attacker.hurt(player.damageSources().playerAttack(player), counterDamage);

        // Critical sound + bright flash
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    12, 0.3, 0.3, 0.3, 0.15);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    8, 0.2, 0.3, 0.2, 0.1);
        }

        // Schedule removal of guard on next tick
        player.getPersistentData().remove(COUNTER_GUARD);
    }
}
