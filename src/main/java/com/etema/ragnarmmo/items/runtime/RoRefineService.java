package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.items.UtilityItems;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.loot.cards.CardEquipType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RoRefineService {
    private static final ResourceLocation RESEARCH_ORIDECON = new ResourceLocation("ragnarmmo", "research_oridecon");

    private RoRefineService() {
    }

    public static RefineQuote quote(ServerPlayer player, ItemStack stack) {
        if (!RagnarConfigs.SERVER.items.refineEnabled.get()) {
            return new RefineQuote(RefineOutcome.DISABLED, 0, 0, ItemStack.EMPTY, 0, 0, 0.0, false, 0, 0);
        }
        if (stack.isEmpty()) {
            return new RefineQuote(RefineOutcome.INVALID_ITEM, 0, 0, ItemStack.EMPTY, 0, 0, 0.0, false, 0, 0);
        }
        if (!RoRefineMath.isRefinable(stack)) {
            return new RefineQuote(RefineOutcome.INVALID_ITEM, 0, 0, ItemStack.EMPTY, 0, 0, 0.0, false, 0, 0);
        }

        int currentLevel = RoItemNbtHelper.getRefineLevel(stack);
        if (currentLevel >= RoItemNbtHelper.MAX_REFINE_LEVEL) {
            return new RefineQuote(RefineOutcome.MAX_REACHED, currentLevel, currentLevel, ItemStack.EMPTY, 0, 0, 0.0, false,
                    0, ZenyWalletHelper.getTotalZeny(player));
        }

        Item material = getRequiredMaterial(stack);
        int materialCount = 1;
        int targetLevel = currentLevel + 1;
        int zenyCost = getZenyCost(stack, targetLevel);
        boolean safe = targetLevel <= RagnarConfigs.SERVER.items.safeRefineLevel.get();
        double successChance = computeSuccessChance(player, stack, targetLevel);
        int availableMaterial = UtilityItems.countItem(player, material);
        int availableZeny = ZenyWalletHelper.getTotalZeny(player);

        return new RefineQuote(RefineOutcome.READY, currentLevel, targetLevel, new ItemStack(material), materialCount, zenyCost,
                successChance, safe, availableMaterial, availableZeny);
    }

    public static RefineResult attempt(ServerPlayer player, ItemStack stack) {
        RefineQuote quote = quote(player, stack);
        if (quote.outcome() != RefineOutcome.READY) {
            return new RefineResult(quote.outcome(), quote, quote.currentLevel());
        }
        if (quote.availableMaterial() < quote.materialCount()) {
            return new RefineResult(RefineOutcome.MISSING_MATERIAL, quote, quote.currentLevel());
        }
        if (quote.availableZeny() < quote.zenyCost()) {
            return new RefineResult(RefineOutcome.MISSING_ZENY, quote, quote.currentLevel());
        }

        if (!UtilityItems.consumeItem(player, quote.material().getItem(), quote.materialCount())) {
            return new RefineResult(RefineOutcome.MISSING_MATERIAL, quote, quote.currentLevel());
        }
        if (!ZenyWalletHelper.tryConsume(player, quote.zenyCost())) {
            ItemStack refund = quote.material().copy();
            refund.setCount(quote.materialCount());
            if (!player.getInventory().add(refund)) {
                player.drop(refund, false);
            }
            return new RefineResult(RefineOutcome.MISSING_ZENY, quote, quote.currentLevel());
        }

        if (quote.safe() || player.getRandom().nextDouble() < quote.successChance()) {
            RoItemNbtHelper.setRefineLevel(stack, quote.targetLevel());
            return new RefineResult(RefineOutcome.SUCCESS, quote, quote.targetLevel());
        }

        int downgradedLevel = Math.max(0, quote.currentLevel() - 1);
        if (downgradedLevel == quote.currentLevel()) {
            return new RefineResult(RefineOutcome.FAILURE_STABLE, quote, quote.currentLevel());
        }

        RoItemNbtHelper.setRefineLevel(stack, downgradedLevel);
        return new RefineResult(RefineOutcome.FAILURE_DOWNGRADE, quote, downgradedLevel);
    }

    private static Item getRequiredMaterial(ItemStack stack) {
        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        return type == CardEquipType.WEAPON ? UtilityItems.ORIDECON.get() : UtilityItems.ELUNIUM.get();
    }

    private static int getZenyCost(ItemStack stack, int targetLevel) {
        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        int baseCost = type == CardEquipType.WEAPON
                ? RagnarConfigs.SERVER.items.weaponBaseCost.get()
                : RagnarConfigs.SERVER.items.armorBaseCost.get();
        return baseCost + Math.max(0, targetLevel - 1) * RagnarConfigs.SERVER.items.costPerLevel.get();
    }

    private static double computeSuccessChance(ServerPlayer player, ItemStack stack, int targetLevel) {
        int safeLevel = RagnarConfigs.SERVER.items.safeRefineLevel.get();
        if (targetLevel <= safeLevel) {
            return 1.0;
        }

        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        boolean weapon = type == CardEquipType.WEAPON;
        double startChance = weapon
                ? RagnarConfigs.SERVER.items.weaponSuccessAfterSafe.get()
                : RagnarConfigs.SERVER.items.armorSuccessAfterSafe.get();
        double penalty = weapon
                ? RagnarConfigs.SERVER.items.weaponSuccessPenaltyPerLevel.get()
                : RagnarConfigs.SERVER.items.armorSuccessPenaltyPerLevel.get();
        int unsafeSteps = targetLevel - safeLevel;
        double chance = startChance - Math.max(0, unsafeSteps - 1) * penalty;

        if (weapon) {
            int researchLevel = PlayerSkillsProvider.get(player)
                    .map(skills -> skills.getSkillLevel(RESEARCH_ORIDECON))
                    .orElse(0);
            chance += researchLevel * RagnarConfigs.SERVER.items.researchOrideconBonusPerLevel.get();
        }

        double min = RagnarConfigs.SERVER.items.minSuccessChance.get();
        return Math.max(min, Math.min(1.0, chance));
    }

    public enum RefineOutcome {
        READY,
        SUCCESS,
        FAILURE_STABLE,
        FAILURE_DOWNGRADE,
        MISSING_MATERIAL,
        MISSING_ZENY,
        MAX_REACHED,
        INVALID_ITEM,
        DISABLED
    }

    public record RefineQuote(
            RefineOutcome outcome,
            int currentLevel,
            int targetLevel,
            ItemStack material,
            int materialCount,
            int zenyCost,
            double successChance,
            boolean safe,
            int availableMaterial,
            int availableZeny) {
    }

    public record RefineResult(
            RefineOutcome outcome,
            RefineQuote quote,
            int finalLevel) {
    }
}
