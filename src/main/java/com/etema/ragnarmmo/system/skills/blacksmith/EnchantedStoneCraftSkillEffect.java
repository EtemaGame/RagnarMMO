package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Enchanted Stone Craft — Active (Blacksmith)
 * RO: Produces Elemental Stones from magic materials.
 * MC: Combines 1 Lapis Lazuli + 3 Iron Ingots in inventory to produce a custom
 *     "Compound Stone" item (a Lapis block with NBT compound_stone=true + name).
 *     These stones can be used later to socket cards or imbue weapons.
 */
public class EnchantedStoneCraftSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "enchanted_stone_craft");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        var inv = player.getInventory();
        // Check for materials: 1 Lapis + 3 Iron Ingots
        int lapisCount = 0, ironCount = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.LAPIS_LAZULI)) lapisCount += s.getCount();
            if (s.is(Items.IRON_INGOT)) ironCount += s.getCount();
        }

        if (lapisCount < 1 || ironCount < 3) {
            player.sendSystemMessage(Component.literal(
                    "§cEnchanted Stone Craft: §fNecesitas §91 Lapis Lazuli §fy §73 Iron Ingots§f."));
            return;
        }

        // Consume materials
        consumeItem(inv, Items.LAPIS_LAZULI, 1);
        consumeItem(inv, Items.IRON_INGOT, 3);

        // Create Compound Stone
        ItemStack stone = new ItemStack(Items.LAPIS_BLOCK);
        CompoundTag nbt = stone.getOrCreateTag();
        nbt.putBoolean("compound_stone", true);
        nbt.putInt("stone_tier", level);
        stone.setHoverName(Component.literal("§9Compound Stone §7[Tier " + level + "]"));

        player.addItem(stone);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.4, 0.4, 0.4, 0.1);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        player.sendSystemMessage(Component.literal("§9✦ Compound Stone §7[Tier " + level + "] §fcreada."));
    }

    private void consumeItem(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
