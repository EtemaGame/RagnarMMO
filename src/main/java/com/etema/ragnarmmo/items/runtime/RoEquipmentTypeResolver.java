package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.items.cards.CardEquipType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;

public final class RoEquipmentTypeResolver {

    private RoEquipmentTypeResolver() {
    }

    public static CardEquipType resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return CardEquipType.ANY;
        }

        Item item = stack.getItem();
        if (item instanceof ShieldItem) {
            return CardEquipType.SHIELD;
        }
        if (item instanceof ElytraItem) {
            return CardEquipType.GARMENT;
        }
        if (item instanceof ArmorItem armor) {
            return switch (armor.getEquipmentSlot()) {
                case HEAD -> CardEquipType.HEADGEAR;
                case FEET -> CardEquipType.SHOES;
                case CHEST, LEGS -> CardEquipType.ARMOR;
                default -> CardEquipType.ARMOR;
            };
        }
        if (item instanceof SwordItem
                || item instanceof TieredItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem) {
            return CardEquipType.WEAPON;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        String path = id != null ? id.getPath() : "";
        if (path.contains("shield") || path.contains("buckler") || path.contains("pavise")
                || path.contains("bulwark") || path.contains("barrier") || path.contains("aegis")) {
            return CardEquipType.SHIELD;
        }
        if (path.contains("ring") || path.contains("brooch") || path.contains("pendant")
                || path.contains("earring") || path.contains("amulet") || path.contains("accessory")
                || path.contains("charm")) {
            return CardEquipType.ACCESSORY;
        }
        if (path.contains("cloak") || path.contains("mantle") || path.contains("garment") || path.contains("cape")) {
            return CardEquipType.GARMENT;
        }
        if (path.contains("shoe") || path.contains("boot") || path.contains("greave")) {
            return CardEquipType.SHOES;
        }
        if (path.contains("helm") || path.contains("helmet") || path.contains("headgear") || path.contains("circlet")) {
            return CardEquipType.HEADGEAR;
        }
        if (path.contains("armor") || path.contains("robe") || path.contains("mail") || path.contains("plate")) {
            return CardEquipType.ARMOR;
        }

        return CardEquipType.ANY;
    }

    public static boolean isCompatible(CardEquipType cardType, ItemStack equipment) {
        if (cardType == null || cardType == CardEquipType.ANY) {
            return true;
        }
        return resolve(equipment) == cardType;
    }
}
