package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Repair Weapon — Active (Blacksmith)
 * RO: Repairs equipment using materials.
 * MC: Repairs the held weapon's durability using iron ingots from inventory.
 *     Each iron ingot restores 30 durability points.
 *     Level increases repair efficiency: (20 + level * 10) per ingot.
 */
public class RepairWeaponSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "repair_weapon");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty() || !weapon.isDamageableItem()) {
            player.sendSystemMessage(Component.literal("§cRepair Weapon: §fSostén un arma/herramienta dañada."));
            return;
        }

        int currentDmg = weapon.getDamageValue();
        if (currentDmg <= 0) {
            player.sendSystemMessage(Component.literal("§7Repair Weapon: El ítem está en perfecto estado."));
            return;
        }

        // Find iron ingots in inventory
        int repairPerIngot = 20 + level * 10;
        var inv = player.getInventory();
        int totalRepaired = 0;

        for (int i = 0; i < inv.getContainerSize() && currentDmg - totalRepaired > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.IRON_INGOT) && !s.isEmpty()) {
                int needed = (int) Math.ceil((double)(currentDmg - totalRepaired) / repairPerIngot);
                int use = Math.min(s.getCount(), needed);
                s.shrink(use);
                totalRepaired += use * repairPerIngot;
            }
        }

        if (totalRepaired == 0) {
            player.sendSystemMessage(Component.literal("§cRepair Weapon: §fNo tienes Iron Ingots."));
            return;
        }

        int newDmg = Math.max(0, currentDmg - totalRepaired);
        weapon.setDamageValue(newDmg);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        player.sendSystemMessage(Component.literal(
                "§7✦ Repair §f" + weapon.getHoverName().getString()
                + " §a+" + totalRepaired + " §fdurabilidad restaurada."));
    }
}
