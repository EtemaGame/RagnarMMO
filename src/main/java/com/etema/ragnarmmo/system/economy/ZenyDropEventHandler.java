package com.etema.ragnarmmo.system.economy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class ZenyDropEventHandler {

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity killed = event.getEntity();
        if (killed.level().isClientSide) return;

        // Only drop Zeny if killed by a player and it's a hostile mob
        if (event.getSource().getEntity() instanceof Player player && isHostile(killed)) {
            double penalty = com.etema.ragnarmmo.system.stats.util.AntiFarmManager.getPenaltyFactor(player);
            List<ItemStack> zenyDrops = ZenyDropManager.calculateDrops(killed, player, killed.getRandom());
            
            for (ItemStack zeny : zenyDrops) {
                if (penalty < 1.0) {
                    zeny.setCount((int)Math.max(1, zeny.getCount() * penalty));
                    // Optional: chance to not drop at all if penalty is very high
                    if (player.getRandom().nextDouble() > penalty) continue;
                }
                ItemEntity entity = new ItemEntity(killed.level(), 
                    killed.getX(), killed.getY(), killed.getZ(), 
                    zeny);
                entity.setDefaultPickUpDelay();
                event.getDrops().add(entity);
            }
        }
    }

    private boolean isHostile(LivingEntity entity) {
        // Hostile monsters and bosses
        return entity instanceof net.minecraft.world.entity.monster.Monster ||
               entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
               entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon ||
               entity instanceof net.minecraft.world.entity.monster.Slime ||
               entity instanceof net.minecraft.world.entity.monster.Phantom ||
               entity instanceof net.minecraft.world.entity.monster.Ghast;
    }
}
