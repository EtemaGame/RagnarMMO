package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.state.CombatCooldownState;

/**
 * Owns logical timing for the combat engine. This layer must remain independent
 * from vanilla melee cooldown semantics.
 */
public class RagnarCombatCooldownService {

    public boolean canUseBasicAttack(CombatCooldownState state, long nowTick) {
        return state != null
                && nowTick >= state.getBasicAttackReadyTick()
                && nowTick >= state.getGlobalDelayReadyTick()
                && nowTick >= state.getAfterCastDelayReadyTick();
    }

    public void markBasicAttackUsed(CombatCooldownState state, long nowTick, int intervalTicks) {
        if (state != null) {
            state.setBasicAttackReadyTick(nowTick + Math.max(1, intervalTicks));
        }
    }

    public boolean canUseSkill(CombatCooldownState state, String skillId, long nowTick) {
        return state != null
                && nowTick >= state.getSkillReadyTick(skillId)
                && nowTick >= state.getGlobalDelayReadyTick()
                && nowTick >= state.getAfterCastDelayReadyTick();
    }

    public void markSkillUsed(CombatCooldownState state, String skillId, long nowTick, int skillCooldownTicks) {
        if (state != null && skillId != null && !skillId.isBlank()) {
            state.setSkillReadyTick(skillId, nowTick + Math.max(0, skillCooldownTicks));
        }
    }

    public void applyGlobalDelay(CombatCooldownState state, long nowTick, int delayTicks) {
        if (state != null) {
            state.setGlobalDelayReadyTick(nowTick + Math.max(0, delayTicks));
        }
    }

    public void applyAfterCastDelay(CombatCooldownState state, long nowTick, int delayTicks) {
        if (state != null) {
            state.setAfterCastDelayReadyTick(nowTick + Math.max(0, delayTicks));
        }
    }
}
