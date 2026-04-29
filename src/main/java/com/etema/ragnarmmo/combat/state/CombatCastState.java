package com.etema.ragnarmmo.combat.state;

import net.minecraft.core.BlockPos;

/**
 * Tracks one active cast, if any, on the server-authoritative combat layer.
 */
public class CombatCastState {
    private String activeSkillId;
    private int activeSkillLevel;
    private long castStartTick;
    private long castEndTick;
    private int variableCastTicks;
    private int fixedCastTicks;
    private int afterCastDelayTicks;
    private int globalDelayTicks;
    private int cooldownTicks;
    private int selectedSlot;
    private boolean offHand;
    private Integer targetEntityId;
    private BlockPos targetPos;

    public boolean isCasting(long nowTick) {
        return activeSkillId != null && nowTick < castEndTick;
    }

    public void clear() {
        activeSkillId = null;
        activeSkillLevel = 0;
        castStartTick = 0L;
        castEndTick = 0L;
        variableCastTicks = 0;
        fixedCastTicks = 0;
        afterCastDelayTicks = 0;
        globalDelayTicks = 0;
        cooldownTicks = 0;
        selectedSlot = 0;
        offHand = false;
        targetEntityId = null;
        targetPos = null;
    }

    public void start(String skillId, int skillLevel, long nowTick, int variableCastTicks, int fixedCastTicks,
            int afterCastDelayTicks, int globalDelayTicks, int cooldownTicks, Integer targetEntityId,
            BlockPos targetPos, int selectedSlot, boolean offHand) {
        this.activeSkillId = skillId;
        this.activeSkillLevel = Math.max(1, skillLevel);
        this.castStartTick = nowTick;
        this.variableCastTicks = Math.max(0, variableCastTicks);
        this.fixedCastTicks = Math.max(0, fixedCastTicks);
        this.afterCastDelayTicks = Math.max(0, afterCastDelayTicks);
        this.globalDelayTicks = Math.max(0, globalDelayTicks);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.castEndTick = nowTick + this.variableCastTicks + this.fixedCastTicks;
        this.targetEntityId = targetEntityId;
        this.targetPos = targetPos;
        this.selectedSlot = selectedSlot;
        this.offHand = offHand;
    }

    public String getActiveSkillId() {
        return activeSkillId;
    }

    public void setActiveSkillId(String activeSkillId) {
        this.activeSkillId = activeSkillId;
    }

    public int getActiveSkillLevel() {
        return activeSkillLevel;
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

    public int getTotalCastTicks() {
        return variableCastTicks + fixedCastTicks;
    }

    public int getAfterCastDelayTicks() {
        return afterCastDelayTicks;
    }

    public int getGlobalDelayTicks() {
        return globalDelayTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isOffHand() {
        return offHand;
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
