package com.etema.ragnarmmo.roitems.hooks.client;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats RO-style tooltip components with proper colors and styling.
 */
public final class RoTooltipFormatter {

    private RoTooltipFormatter() {
    }

    private static final String SEPARATOR = "----------------------";


    /**
     * Add RO-style tooltip lines to an item's tooltip.
     *
     * @param tooltip the tooltip list to add to
     * @param stack   the item stack
     * @param rule    the item rule with data to display
     * @param player  local client player, may be null
     */
    public static void addTooltipLines(List<Component> tooltip, ItemStack stack, RoItemRule rule,
            Player player) {
        if (rule == null || rule.isEmpty())
            return;

        tooltip.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.DARK_GRAY));

        // Combat stats (ATK, ASPD, DEF) calculated from item attribute modifiers
        List<Component> combatLines = buildCombatLines(stack);
        if (!combatLines.isEmpty()) {
            tooltip.add(sectionTitle("Combat Stats"));
            tooltip.addAll(combatLines);
        }

        if (rule.hasAttributeBonuses()) {
            tooltip.add(sectionTitle("Bonus Stats"));
            rule.attributeBonuses().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> tooltip.add(formatStatBonus(entry.getKey(), entry.getValue())));
        }

        if (rule.hasRequirements()) {
            tooltip.add(Component.literal(""));
            tooltip.add(sectionTitle("Requirements"));
            RequirementState requirementState = RequirementState.from(player, rule);

            if (rule.requiredBaseLevel() > 0) {
                Component levelText = Component.translatable("tooltip.ragnarmmo.roitems.required_level",
                        rule.requiredBaseLevel());
                tooltip.add(formatRequirement(levelText, requirementState.levelMet()));
            }

            if (!rule.allowedJobs().isEmpty()) {
                String classes = rule.allowedJobs().stream()
                        .map(JobType::getDisplayName)
                        .collect(Collectors.joining(", "));

                Component classText = Component.translatable("tooltip.ragnarmmo.roitems.classes", classes);
                tooltip.add(formatRequirement(classText, requirementState.classMet()));
            }
        }

        if (rule.cardSlots() > 0) {
            tooltip.add(Component.literal(""));
            java.util.List<String> slottedCards = RoItemNbtHelper.getSlottedCards(stack);
            tooltip.add(formatCardSlots(rule.cardSlots(), slottedCards.size()));

            for (int i = 0; i < rule.cardSlots(); i++) {
                if (i < slottedCards.size()) {
                    var cardDef = com.etema.ragnarmmo.system.loot.cards.CardRegistry.getInstance()
                            .get(slottedCards.get(i));
                    String cardName = cardDef != null ? cardDef.displayName() : "Unknown Card";
                    tooltip.add(Component.literal("  - " + cardName).withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.literal("  - Empty Slot").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }


    }

    /**
     * Format a stat bonus for display.
     *
     * @param stat  the stat key
     * @param value the bonus value
     * @return formatted component
     */
    public static Component formatStatBonus(StatKeys stat, int value) {
        String sign = value >= 0 ? "+" : "";
        ChatFormatting color = value >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        MutableComponent statName = Component.translatable("stat.ragnarmmo." + stat.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GRAY);
        return Component.literal("  ")
                .append(statName)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(sign + value).withStyle(color));
    }

    private static Component formatRequirement(Component text, Boolean met) {
        String prefix;
        ChatFormatting color;

        if (met == null) {
            prefix = "[?] ";
            color = ChatFormatting.DARK_GRAY;
        } else if (met) {
            prefix = "[OK] ";
            color = ChatFormatting.DARK_GREEN;
        } else {
            prefix = "[X] ";
            color = ChatFormatting.RED;
        }

        return Component.literal("  ")
                .append(Component.literal(prefix).withStyle(color))
                .append(text.copy().withStyle(color));
    }

    private static Component sectionTitle(String title) {
        return Component.literal(title).withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD);
    }



    private static Component formatCardSlots(int slots, int filled) {
        StringBuilder visual = new StringBuilder("[");
        for (int i = 0; i < slots; i++) {
            if (i > 0) {
                visual.append(' ');
            }
            visual.append(i < filled ? 'X' : 'o');
        }
        visual.append(']');

        return Component.literal("Slots: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(visual.toString()).withStyle(ChatFormatting.AQUA));
    }



    private static List<Component> buildCombatLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        Multimap<Attribute, AttributeModifier> mainhandModifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

        if (mainhandModifiers.containsKey(Attributes.ATTACK_DAMAGE)) {
            // Vanilla base is 1.0, attributes are modifiers
            double damage = 1.0D + sumAttribute(mainhandModifiers, Attributes.ATTACK_DAMAGE);
            if (damage > 0) {
                lines.add(formatCombatLine("ATK", String.valueOf(Math.round(damage))));
            }
        }

        if (mainhandModifiers.containsKey(Attributes.ATTACK_SPEED)) {
            double speed = 4.0D + sumAttribute(mainhandModifiers, Attributes.ATTACK_SPEED);
            if (speed > 0) {
                lines.add(formatCombatLine("ASPD", String.format(Locale.ROOT, "%.1f", speed)));
            }
        }

        // Add Range (Reach Distance) - Essential for Better Combat
        if (mainhandModifiers.containsKey(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get())) {
            double reach = 3.0D + sumAttribute(mainhandModifiers, net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
            lines.add(formatCombatLine("Range", String.format(Locale.ROOT, "%.1f", reach)));
        }

        if (stack.getItem() instanceof ArmorItem armor) {
            Multimap<Attribute, AttributeModifier> armorModifiers = stack
                    .getAttributeModifiers(armor.getEquipmentSlot());
            if (armorModifiers.containsKey(Attributes.ARMOR)) {
                double defense = sumAttribute(armorModifiers, Attributes.ARMOR);
                if (defense > 0) {
                    int displayDef = (int) Math.round(defense * 5);
                    lines.add(formatCombatLine("DEF", "+" + displayDef));
                }
            }
        }

        return lines;
    }

    private static double sumAttribute(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
        double total = 0.0D;
        for (AttributeModifier mod : modifiers.get(attribute)) {
            total += mod.getAmount();
        }
        return total;
    }

    private static Component formatCombatLine(String label, String value) {
        return Component.literal("  ")
                .append(Component.literal(label).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private record RequirementState(Boolean levelMet, Boolean classMet) {
        private static RequirementState from(Player player, RoItemRule rule) {
            if (player == null) {
                return new RequirementState(null, null);
            }

            var statsOpt = RagnarCoreAPI.get(player);
            if (statsOpt.isEmpty()) {
                return new RequirementState(null, null);
            }
            IPlayerStats stats = statsOpt.get();

            Boolean levelOk = null;
            if (rule.requiredBaseLevel() > 0) {
                levelOk = stats.getLevel() >= rule.requiredBaseLevel();
            }

            Boolean classOk = null;
            if (!rule.allowedJobs().isEmpty()) {
                JobType playerJob = JobType.fromId(stats.getJobId());
                classOk = rule.allowedJobs().contains(playerJob);
            }

            return new RequirementState(levelOk, classOk);
        }
    }
}
