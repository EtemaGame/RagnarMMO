package com.etema.ragnarmmo.combat.contract;

import com.etema.ragnarmmo.combat.element.ElementType;

public record SkillCombatSpec(
        RagnarDamageType damageType,
        ElementType element,
        SkillHitPolicy hitPolicy,
        double damagePercent,
        int hitCount,
        double aoeRadius,
        double splashRatio) {
    public SkillCombatSpec {
        damageType = damageType == null ? RagnarDamageType.PHYSICAL : damageType;
        element = element == null ? ElementType.NEUTRAL : element;
        hitPolicy = hitPolicy == null ? SkillHitPolicy.BASIC_ATTACK : hitPolicy;
        damagePercent = Math.max(0.0D, damagePercent);
        hitCount = Math.max(1, hitCount);
        aoeRadius = Math.max(0.0D, aoeRadius);
        splashRatio = Math.max(0.0D, splashRatio);
    }
}
