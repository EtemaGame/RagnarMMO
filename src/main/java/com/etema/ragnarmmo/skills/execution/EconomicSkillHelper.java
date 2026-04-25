package com.etema.ragnarmmo.skills.execution;

import com.etema.ragnarmmo.skills.data.SkillDefinition;

public final class EconomicSkillHelper {
    private EconomicSkillHelper() {
    }

    public static double vendorBuyDiscount(SkillDefinition definition, int level) {
        return definition != null ? definition.getLevelDouble("vendor_buy_discount", level, 0.0D) : 0.0D;
    }

    public static double vendorSellBonus(SkillDefinition definition, int level) {
        return definition != null ? definition.getLevelDouble("vendor_sell_bonus", level, 0.0D) : 0.0D;
    }

    public static int zenyCost(SkillDefinition definition, int level, int defaultValue) {
        return definition != null ? definition.getLevelInt("zeny_cost", level, defaultValue) : defaultValue;
    }
}
