package com.etema.ragnarmmo.combat.timing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.etema.ragnarmmo.combat.state.CombatCastState;
import com.etema.ragnarmmo.combat.state.CombatCooldownState;
import com.etema.ragnarmmo.combat.engine.RagnarCombatCooldownService;
import com.etema.ragnarmmo.skills.api.SkillCategory;
import com.etema.ragnarmmo.skills.api.SkillTier;
import com.etema.ragnarmmo.skills.api.SkillUsageType;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillLevelData;

import net.minecraft.resources.ResourceLocation;

class CombatTimingStateTest {
    @Test
    void skillDefinitionSeparatesVariableFixedAfterCastGlobalAndCooldownTiming() {
        SkillDefinition skill = SkillDefinition.builder(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "timing_test"))
                .category(SkillCategory.CLASS_PASSIVE)
                .tier(SkillTier.FIRST)
                .usageType(SkillUsageType.ACTIVE)
                .castTimeTicks(60)
                .castDelayTicks(12)
                .cooldownTicks(80)
                .levelData(Map.of(3, new SkillLevelData(
                        Map.of(
                                "variable_cast_ticks", 40.0,
                                "fixed_cast_ticks", 10.0,
                                "after_cast_delay_ticks", 14.0,
                                "global_delay_ticks", 6.0,
                                "cooldown_ticks", 90.0),
                        Map.of(),
                        Map.of())))
                .build();

        assertEquals(60, skill.getVariableCastTicks(1));
        assertEquals(0, skill.getFixedCastTicks(1));
        assertEquals(12, skill.getAfterCastDelayTicks(1));
        assertEquals(0, skill.getGlobalDelayTicks(1));
        assertEquals(80, skill.getCooldownTicks(1));

        assertEquals(40, skill.getVariableCastTicks(3));
        assertEquals(10, skill.getFixedCastTicks(3));
        assertEquals(14, skill.getAfterCastDelayTicks(3));
        assertEquals(6, skill.getGlobalDelayTicks(3));
        assertEquals(90, skill.getCooldownTicks(3));
    }

    @Test
    void cooldownStateBlocksActionsForAttackGlobalSkillAndAfterCastWindows() {
        RagnarCombatCooldownService service = new RagnarCombatCooldownService();
        CombatCooldownState state = new CombatCooldownState();

        assertTrue(service.canUseBasicAttack(state, 10));
        assertTrue(service.canUseSkill(state, "ragnarmmo:test", 10));

        service.markBasicAttackUsed(state, 10, 5);
        assertFalse(service.canUseBasicAttack(state, 14));
        assertTrue(service.canUseBasicAttack(state, 15));

        service.applyGlobalDelay(state, 20, 10);
        assertFalse(service.canUseBasicAttack(state, 25));
        assertFalse(service.canUseSkill(state, "ragnarmmo:test", 25));
        assertTrue(service.canUseBasicAttack(state, 30));

        service.applyAfterCastDelay(state, 40, 8);
        assertFalse(service.canUseBasicAttack(state, 47));
        assertFalse(service.canUseSkill(state, "ragnarmmo:test", 47));
        assertTrue(service.canUseSkill(state, "ragnarmmo:test", 48));

        service.markSkillUsed(state, "ragnarmmo:test", 50, 12);
        assertFalse(service.canUseSkill(state, "ragnarmmo:test", 61));
        assertTrue(service.canUseSkill(state, "ragnarmmo:test", 62));
    }

    @Test
    void castStateStoresServerAuthoritativeCastAndDelayContext() {
        CombatCastState cast = new CombatCastState();
        cast.start("ragnarmmo:test", 4, 100, 30, 10, 8, 6, 40, 123, null, 2, false);

        assertTrue(cast.isCasting(139));
        assertFalse(cast.isCasting(140));
        assertEquals("ragnarmmo:test", cast.getActiveSkillId());
        assertEquals(4, cast.getActiveSkillLevel());
        assertEquals(40, cast.getTotalCastTicks());
        assertEquals(8, cast.getAfterCastDelayTicks());
        assertEquals(6, cast.getGlobalDelayTicks());
        assertEquals(40, cast.getCooldownTicks());
        assertEquals(123, cast.getTargetEntityId());
        assertEquals(2, cast.getSelectedSlot());
    }
}
