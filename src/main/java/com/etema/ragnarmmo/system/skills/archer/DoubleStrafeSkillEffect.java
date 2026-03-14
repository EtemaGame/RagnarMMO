package com.etema.ragnarmmo.system.skills.archer;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class DoubleStrafeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "double_strafe");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Double Strafe fires two arrows simultaneously or in very rapid succession.
        // In MC, we can spawn two arrows with slightly different trajectories or just
        // one after another.

        // Base damage in RO: 100% + 10% * level, hits twice.
        // Total damage: (1 + 0.1 * level) * 2.

        // Ensure player is using a bow? Archer skills usually require a bow.
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() != Items.BOW && mainHand.getItem() != Items.CROSSBOW) {
            // In a real mod, we might send a message "Requires a bow".
            return;
        }

        // Spawn 2 arrows in rapid sequence (2 ticks apart)
        for (int i = 0; i < 2; i++) {
            final int index = i;
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(i * 2, () -> {
                Arrow arrow = new Arrow(player.level(), player);
                arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
    
                Vec3 look = player.getLookAngle();
                double spread = 0.05; 
                arrow.shoot(look.x, look.y, look.z, 3.0f, (float) spread);
    
                double damageBonus = 1.0 + (level * 0.1);
                arrow.setBaseDamage(arrow.getBaseDamage() * damageBonus);
    
                player.level().addFreshEntity(arrow);
                
                // Sound on each shot
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F,
                        1.2F + (index * 0.2F));

                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, 
                        player.getX() + look.x, player.getY() + 1.5, player.getZ() + look.z, 
                        5, 0.1, 0.1, 0.1, 0.05);
                }
            });
        }

        // Sound effect (simulated)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F,
                1.0F / (player.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
    }
}
