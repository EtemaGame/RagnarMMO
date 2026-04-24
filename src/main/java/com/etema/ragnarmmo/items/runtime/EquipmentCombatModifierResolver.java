package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.system.loot.cards.CardRegistry;
import com.etema.ragnarmmo.mobs.util.MobUtils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves combat multipliers granted by equipped slotted cards.
 *
 * <p>This gives the tag-based race/element/size taxonomy real gameplay weight
 * without introducing a second hardcoded combat table for each skill.</p>
 */
public final class EquipmentCombatModifierResolver {

    private EquipmentCombatModifierResolver() {
    }

    public static double getOutgoingModifier(Player player, LivingEntity target, ElementType attackElement, boolean magic) {
        if (player == null || target == null) {
            return 1.0;
        }

        String raceId = CombatPropertyResolver.getRaceId(target);
        String sizeId = CombatPropertyResolver.getSizeId(target);
        String elementId = CombatPropertyResolver.getElementId(CombatPropertyResolver.getDefensiveElement(target));
        boolean boss = MobUtils.isBossLikeForCompatibility(target);

        double modifier = 1.0;
        for (ItemStack stack : getEquippedItems(player)) {
            modifier *= getOutgoingModifierFromStack(stack, raceId, sizeId, elementId, boss, magic);
        }
        return Math.max(0.0, modifier);
    }

    public static double getIncomingModifier(Player player, LivingEntity attacker, ElementType attackElement, boolean magic) {
        if (player == null) {
            return 1.0;
        }

        String raceId = attacker != null ? CombatPropertyResolver.getRaceId(attacker) : "";
        String sizeId = attacker != null ? CombatPropertyResolver.getSizeId(attacker) : "";
        String elementId = CombatPropertyResolver.getElementId(attackElement);
        boolean boss = attacker != null && MobUtils.isBossLikeForCompatibility(attacker);

        double modifier = 1.0;
        for (ItemStack stack : getEquippedItems(player)) {
            modifier *= getIncomingModifierFromStack(stack, raceId, sizeId, elementId, boss, magic);
        }
        return Math.max(0.0, modifier);
    }

    private static List<ItemStack> getEquippedItems(Player player) {
        List<ItemStack> equipped = new ArrayList<>(6);
        if (!player.getMainHandItem().isEmpty()) {
            equipped.add(player.getMainHandItem());
        }
        if (!player.getOffhandItem().isEmpty()) {
            equipped.add(player.getOffhandItem());
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty()) {
                equipped.add(armor);
            }
        }
        return equipped;
    }

    private static double getOutgoingModifierFromStack(ItemStack stack, String raceId, String sizeId, String elementId,
            boolean boss, boolean magic) {
        double modifier = 1.0;
        for (String cardId : RoItemNbtHelper.getSlottedCards(stack)) {
            var cardDef = CardRegistry.getInstance().get(cardId);
            if (cardDef == null || cardDef.modifiers() == null || cardDef.modifiers().isEmpty()) {
                continue;
            }
            modifier *= getOutgoingModifier(cardDef.modifiers(), raceId, sizeId, elementId, boss, magic);
        }
        return modifier;
    }

    private static double getIncomingModifierFromStack(ItemStack stack, String raceId, String sizeId, String elementId,
            boolean boss, boolean magic) {
        double modifier = 1.0;
        for (String cardId : RoItemNbtHelper.getSlottedCards(stack)) {
            var cardDef = CardRegistry.getInstance().get(cardId);
            if (cardDef == null || cardDef.modifiers() == null || cardDef.modifiers().isEmpty()) {
                continue;
            }
            modifier *= getIncomingModifier(cardDef.modifiers(), raceId, sizeId, elementId, boss, magic);
        }
        return modifier;
    }

    private static double getOutgoingModifier(Map<String, Double> modifiers, String raceId, String sizeId,
            String elementId, boolean boss, boolean magic) {
        double modifier = 1.0;

        modifier *= 1.0 + getBonus(modifiers, "damage_vs_race_" + raceId);
        modifier *= 1.0 + getBonus(modifiers, "damage_vs_size_" + sizeId);
        modifier *= 1.0 + getBonus(modifiers, "damage_vs_element_" + elementId);

        if (magic) {
            modifier *= 1.0 + getBonus(modifiers, "magic_damage_vs_race_" + raceId);
            modifier *= 1.0 + getBonus(modifiers, "magic_damage_vs_size_" + sizeId);
            modifier *= 1.0 + getBonus(modifiers, "magic_damage_vs_element_" + elementId);
            if (boss) {
                modifier *= 1.0 + getBonus(modifiers, "magic_damage_vs_boss");
            }
        } else {
            modifier *= 1.0 + getBonus(modifiers, "physical_damage_vs_race_" + raceId);
            modifier *= 1.0 + getBonus(modifiers, "physical_damage_vs_size_" + sizeId);
            modifier *= 1.0 + getBonus(modifiers, "physical_damage_vs_element_" + elementId);
            if (boss) {
                modifier *= 1.0 + getBonus(modifiers, "physical_damage_vs_boss");
            }
        }

        if (boss) {
            modifier *= 1.0 + getBonus(modifiers, "damage_vs_boss");
        }

        return Math.max(0.0, modifier);
    }

    private static double getIncomingModifier(Map<String, Double> modifiers, String raceId, String sizeId,
            String elementId, boolean boss, boolean magic) {
        double modifier = 1.0;

        modifier *= 1.0 - getBonus(modifiers, "resist_race_" + raceId);
        modifier *= 1.0 - getBonus(modifiers, "resist_size_" + sizeId);
        modifier *= 1.0 - getBonus(modifiers, "resist_element_" + elementId);

        if (magic) {
            modifier *= 1.0 - getBonus(modifiers, "magic_resist_race_" + raceId);
            modifier *= 1.0 - getBonus(modifiers, "magic_resist_size_" + sizeId);
            modifier *= 1.0 - getBonus(modifiers, "magic_resist_element_" + elementId);
            if (boss) {
                modifier *= 1.0 - getBonus(modifiers, "magic_resist_boss");
            }
        } else {
            modifier *= 1.0 - getBonus(modifiers, "physical_resist_race_" + raceId);
            modifier *= 1.0 - getBonus(modifiers, "physical_resist_size_" + sizeId);
            modifier *= 1.0 - getBonus(modifiers, "physical_resist_element_" + elementId);
            if (boss) {
                modifier *= 1.0 - getBonus(modifiers, "physical_resist_boss");
            }
        }

        if (boss) {
            modifier *= 1.0 - getBonus(modifiers, "resist_boss");
        }

        return Math.max(0.0, modifier);
    }

    private static double getBonus(Map<String, Double> modifiers, String keySuffix) {
        if (keySuffix == null || keySuffix.endsWith("_")) {
            return 0.0;
        }
        return modifiers.getOrDefault("ragnarmmo:" + keySuffix, 0.0);
    }
}
