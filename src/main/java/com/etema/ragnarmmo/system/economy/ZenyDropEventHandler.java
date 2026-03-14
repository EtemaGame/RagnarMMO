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

        // Only drop Zeny if killed by a player
        if (event.getSource().getEntity() instanceof Player player) {
            List<ItemStack> zenyDrops = ZenyDropManager.calculateDrops(killed, player, killed.getRandom());
            
            for (ItemStack zeny : zenyDrops) {
                ItemEntity entity = new ItemEntity(killed.level(), 
                    killed.getX(), killed.getY(), killed.getZ(), 
                    zeny);
                entity.setDefaultPickUpDelay();
                event.getDrops().add(entity);
            }
        }
    }
}
