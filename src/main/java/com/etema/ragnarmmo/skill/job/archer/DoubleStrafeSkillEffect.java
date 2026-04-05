package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Double Strafe — Active ranged skill (Archer).
 *
 * <p><b>RO Formula:</b> 2 arrows, each dealing {@code (100 + 10 × level)} % WATK.
 * Total damage = {@code (200 + 20 × level)} % WATK.
 *
 * <p>Arrow entities naturally bypass {@code hurtTime} because they are separate damage
 * events with their own entity. No {@link com.etema.ragnarmmo.combat.damage.SkillDamageHelper}
 * hack needed here. The damage multiplier is applied directly to the arrow's base damage
 * as a percentage of the weapon's ATK contribution.
 *
 * <p>Requires Bow or Crossbow in main hand.
 */
public class DoubleStrafeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "double_strafe");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() != Items.BOW && mainHand.getItem() != Items.CROSSBOW) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("message.ragnarmmo.requires_bow")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        // RO: each arrow = (100 + 10 × level)% WATK
        // We express this as a ratio applied to the arrow's default base damage (2.0)
        // relative to the player's actual weapon ATK so it scales with gear.
        float pctPerHit = 100.0f + (10.0f * level); // e.g. Lv1=110%, Lv10=200%
        float scaledDamage = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, pctPerHit));
        // Arrow base damage is in half-hearts per hit; convert from our ATK unit
        double arrowBaseDamage = scaledDamage;

        for (int i = 0; i < 2; i++) {
            final int index = i;
            SkillSequencer.schedule(i * 3, () -> {
                Arrow arrow = new Arrow(player.level(), player);
                Vec3 look = player.getLookAngle();

                arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                // Slight vertical divergence on the second arrow for the "double" look
                double spreadY = (index == 1) ? 0.005 : 0.0;
                arrow.shoot(look.x, look.y + spreadY, look.z, 3.0f, 0.3f);
                arrow.setBaseDamage(arrowBaseDamage);
                arrow.setCritArrow(true); // Visual crit stars to indicate skill shot

                player.level().addFreshEntity(arrow);

                // RO-style high-pitched twang
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        0.7f, 1.3f + index * 0.2f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT,
                            player.getX() + look.x * 1.5,
                            player.getY() + 1.2,
                            player.getZ() + look.z * 1.5,
                            6, 0.1, 0.1, 0.1, 0.05);
                }
            });
        }
    }
}
