package com.etema.ragnarmmo.combat.state;

import net.minecraft.core.BlockPos;

/**
 * Tracks one active cast, if any, on the server-authoritative combat layer.
 */
public class CombatCastState {
    private String activeSkillId;
    private long castStartTick;
    private long castEndTick;
    private Integer targetEntityId;
    private BlockPos targetPos;

    public boolean isCasting(long nowTick) {
        return activeSkillId != null && nowTick < castEndTick;
    }

    public void clear() {
        activeSkillId = null;
        castStartTick = 0L;
        castEndTick = 0L;
        targetEntityId = null;
        targetPos = null;
    }

    public String getActiveSkillId() {
        return activeSkillId;
    }

    public void setActiveSkillId(String activeSkillId) {
        this.activeSkillId = activeSkillId;
    }

    public long getCastStartTick() {
        return castStartTick;
    }

    public void setCastStartTick(long castStartTick) {
        this.castStartTick = castStartTick;
    }

    public long getCastEndTick() {
        return castEndTick;
    }

    public void setCastEndTick(long castEndTick) {
        this.castEndTick = castEndTick;
    }

    public Integer getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Integer targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos;
    }
}
