package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.execution.projectile.RagnarArrowSpawnHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Beast Strafing — Passive
 * RO: Fires two additional arrows when attacking a Brute monster.
 *     In higher RO versions: rapid-fires against beast/insect type enemies.
 *
 * Minecraft:
 *  - onOffensiveHurt: when an arrow hits a CREATURE or Arthropod mob,
 *    spawns 2 additional arrows at the target with slight spread.
 *  - Level scales arrow damage bonus per extra arrow.
 */
public class BeastStrafingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "beast_strafing");

    private static final java.util.Map<java.util.UUID, Long> STRAFING_STANCE = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;

        LivingEntity target = event.getEntity();
        MobCategory cat = target.getType().getCategory();
        
        // Block friendly animals
        if (cat == MobCategory.CREATURE || cat == MobCategory.WATER_CREATURE) return;

        boolean isBruteOrArthropod = target.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD ||
                (target instanceof net.minecraft.world.entity.monster.Monster && target.getMobType() != net.minecraft.world.entity.MobType.UNDEAD);
        
        if (!isBruteOrArthropod) return;

        // Grant the stance for 3 seconds (60 ticks)
        STRAFING_STANCE.put(player.getUUID(), player.level().getGameTime() + 60);
        
        // Visual indicator that the skill is ready
        if (player.level() instanceof ServerLevel sl && player.tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 2, player.getZ(),
                    3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        Long expiry = STRAFING_STANCE.get(player.getUUID());
        if (expiry == null || player.level().getGameTime() > expiry) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_requires_beast_stance")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        // Consume the stance
        STRAFING_STANCE.remove(player.getUUID());

        double baseAtk = com.etema.ragnarmmo.common.api.stats.StatAttributes.getTotal(player, com.etema.ragnarmmo.common.api.stats.StatKeys.DEX);
        float bonusDmg = (float) (baseAtk * (1.2f + level * 0.1f));
        int extraArrows = Math.min(level / 3 + 1, 3);

        for (int i = 0; i < extraArrows; i++) {
            double spread = 0.05 * (i + 1);
            Vec3 look = player.getLookAngle();
            Vec3 direction = new Vec3(
                    look.x + player.getRandom().nextGaussian() * spread,
                    look.y + player.getRandom().nextGaussian() * spread,
                    look.z + player.getRandom().nextGaussian() * spread);
            RagnarArrowSpawnHelper.spawn(player, direction, 3.5F, 0.0F, 1.0F,
                    arrow -> {
                        arrow.setBaseDamage(bonusDmg);
                        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }, snapshot -> {
                        snapshot.putBoolean("bypass_iframes", true);
                    });
        }
    }
}
