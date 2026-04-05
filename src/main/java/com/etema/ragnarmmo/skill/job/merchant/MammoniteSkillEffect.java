package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.roitems.ZenyItems;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Mammonite — Active (Merchant)
 * RO: Pay Zeny to deal increased melee damage.
 *
 * MC adaptation: Costs Gold Zeny from the player's inventory instead.
 *  Level 1 = 1 Gold Zeny → 1.5× melee damage
 *  Level 5 = 5 Gold Zeny → 3.5× melee damage
 *  Level 10 = 10 Gold Zeny → 6.0× melee damage
 */
public class MammoniteSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "mammonite");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int cost = level; // Gold Zeny

        // Check inventory for Gold Zeny
        int found = player.getInventory().countItem(ZenyItems.GOLD_ZENY.get());
        if (found < cost) {
            player.sendSystemMessage(Component.literal(
                    "§c✖ Mammonite needs §e" + cost + " Gold Zeny §c(you have §e" + found + "§c)"));
            return;
        }

        // Find melee target first
        net.minecraft.world.entity.LivingEntity target = getMeleeTarget(player);
        if (target == null) {
            player.sendSystemMessage(Component.literal("§c✖ No target in range."));
            return;
        }

        // Consume emeralds
        int remaining = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(ZenyItems.GOLD_ZENY.get())) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
        }

        // RO: (100 + 50×level)% ATK per Gold Zeny invested
        float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, 100f + 50f * level));
        // Introduce a delay for the "weighted" feel (swinging a bag of money)
        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(3, () -> {
            if (!target.isAlive()) return;

            target.hurt(player.damageSources().mobAttack(player), damage);

            // SFX/VFX
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0f, 1.4f);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.6f);
            
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                // Shimmering gold burst using multiple particle types
                sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(ZenyItems.GOLD_ZENY.get())),
                        target.getX(), target.getY() + 1.2, target.getZ(), 30, 0.3, 0.3, 0.3, 0.15);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.4, 0.2, 0.1);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_OFF,
                        target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.4, 0.2, 0.05);
            }

            player.displayClientMessage(Component.literal("§6-" + cost + " 💰 §c⚔ Mammonite!"), true);
        });
    }

    private net.minecraft.world.entity.LivingEntity getMeleeTarget(ServerPlayer player) {
        net.minecraft.world.phys.Vec3 start = player.getEyePosition();
        net.minecraft.world.phys.Vec3 end = start.add(player.getLookAngle().scale(3.5));
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(3.5);
        net.minecraft.world.entity.LivingEntity closest = null;
        double dist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.LivingEntity e :
                player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        box, e -> e != player && e.isAlive())) {
            var hit = e.getBoundingBox().inflate(0.5).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(e.position());
                if (d < dist) { dist = d; closest = e; }
            }
        }
        return closest;
    }
}
