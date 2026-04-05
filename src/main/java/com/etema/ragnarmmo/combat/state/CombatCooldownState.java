package com.etema.ragnarmmo.combat.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side logical cooldown state. This is intentionally independent from
 * vanilla's melee cooldown semantics.
 */
public class CombatCooldownState {
    private long basicAttackReadyTick;
    private long globalDelayReadyTick;
    private long afterCastDelayReadyTick;
    private final Map<String, Long> skillReadyTicks = new HashMap<>();

    public long getBasicAttackReadyTick() {
        return basicAttackReadyTick;
    }

    public void setBasicAttackReadyTick(long basicAttackReadyTick) {
        this.basicAttackReadyTick = basicAttackReadyTick;
    }

    public long getGlobalDelayReadyTick() {
        return globalDelayReadyTick;
    }

    public void setGlobalDelayReadyTick(long globalDelayReadyTick) {
        this.globalDelayReadyTick = globalDelayReadyTick;
    }

    public long getAfterCastDelayReadyTick() {
        return afterCastDelayReadyTick;
    }

    public void setAfterCastDelayReadyTick(long afterCastDelayReadyTick) {
        this.afterCastDelayReadyTick = afterCastDelayReadyTick;
    }

    public long getSkillReadyTick(String skillId) {
        return skillReadyTicks.getOrDefault(skillId, 0L);
    }

    public void setSkillReadyTick(String skillId, long readyTick) {
        if (skillId != null && !skillId.isBlank()) {
            skillReadyTicks.put(skillId, readyTick);
        }
    }
}
