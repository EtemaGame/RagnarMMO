package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraftforge.fml.common.Mod;

/**
 * Optimized access layer for RO Item settings.
 * Centralizes defaults and access logic.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RoItemsConfigAccess {

    public static boolean isEnabled() { return RagnarConfigs.SERVER.items.enabled.get(); }
    public static boolean isHeuristicsEnabled() { return RagnarConfigs.SERVER.items.enableHeuristics.get(); }
    public static boolean blockEquipOnRestriction() { return RagnarConfigs.SERVER.items.blockEquipOnRestriction.get(); }
    public static boolean reduceDamageOnRestriction() { return RagnarConfigs.SERVER.items.reduceDamageOnRestriction.get(); }
    public static int getTickCheckInterval() { return RagnarConfigs.SERVER.items.tickCheckInterval.get(); }
    public static int getMessageCooldownMs() { return RagnarConfigs.SERVER.items.messageCooldownMs.get(); }
    public static boolean showTooltips() { return RagnarConfigs.SERVER.items.showTooltips.get(); }
    public static double getPenaltyDamage() { return RagnarConfigs.SERVER.items.penaltyDamage.get(); }

    // Refine Settings
    public static boolean isRefineEnabled() { return RagnarConfigs.SERVER.items.refineEnabled.get(); }
    public static int getSafeRefineLevel() { return RagnarConfigs.SERVER.items.safeRefineLevel.get(); }
    public static int getWeaponBaseCost() { return RagnarConfigs.SERVER.items.weaponBaseCost.get(); }
    public static int getArmorBaseCost() { return RagnarConfigs.SERVER.items.armorBaseCost.get(); }
    public static int getCostPerLevel() { return RagnarConfigs.SERVER.items.costPerLevel.get(); }
    public static double getWeaponSuccessAfterSafe() { return RagnarConfigs.SERVER.items.weaponSuccessAfterSafe.get(); }
    public static double getArmorSuccessAfterSafe() { return RagnarConfigs.SERVER.items.armorSuccessAfterSafe.get(); }
    public static double getWeaponSuccessPenaltyPerLevel() { return RagnarConfigs.SERVER.items.weaponSuccessPenaltyPerLevel.get(); }
    public static double getArmorSuccessPenaltyPerLevel() { return RagnarConfigs.SERVER.items.armorSuccessPenaltyPerLevel.get(); }
    public static double getMinSuccessChance() { return RagnarConfigs.SERVER.items.minSuccessChance.get(); }
    public static double getResearchOrideconBonusPerLevel() { return RagnarConfigs.SERVER.items.researchOrideconBonusPerLevel.get(); }
}
