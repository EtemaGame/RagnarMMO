package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;

/**
 * Aqua Benedicta — Active (Acolyte)
 * RO: Creates Holy Water when cast while standing in water.
 * MC: If the player is in/adjacent to water, converts a Water Bottle in inventory
 *     into a "Holy Water" bottle (Water Bottle + NBT holy_water=true).
 *     Holy Water is later consumed by Aspersion (Priest).
 */
public class AquaBenedictaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "aqua_benedicta");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Must be in or touching water
        BlockPos pos = player.blockPosition();
        boolean inWater = player.isInWater()
                || player.level().getBlockState(pos).is(Blocks.WATER)
                || player.level().getBlockState(pos.below()).is(Blocks.WATER);

        if (!inWater) {
            player.sendSystemMessage(Component.literal("§cAqua Benedicta: §fDebes estar en agua para usarla."));
            return;
        }

        // Find a water bottle in inventory
        var inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.POTION)) {
                String potionTag = s.getOrCreateTag().getString("Potion");
                if (potionTag.equals("minecraft:water")) {
                    slot = i;
                    break;
                }
            }
        }

        if (slot == -1) {
            player.sendSystemMessage(Component.literal("§cAqua Benedicta: §fNecesitas un Water Bottle en inventario."));
            return;
        }

        // Convert to Holy Water
        ItemStack waterBottle = inv.getItem(slot).copy();
        waterBottle.setCount(1);
        CompoundTag nbt = waterBottle.getOrCreateTag();
        nbt.putBoolean("holy_water", true);
        waterBottle.setHoverName(Component.literal("§fHoly Water"));
        inv.getItem(slot).shrink(1);
        player.addItem(waterBottle);

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
        player.sendSystemMessage(Component.literal("§b✦ Holy Water §fcreada."));
    }
}
