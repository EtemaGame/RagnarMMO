package com.etema.ragnarmmo.client.effects.runtime;

public final class EffectPlaybackState {
    private int ageTicks;
    private int lastRuntimeExecutionAge = Integer.MIN_VALUE;

    public EffectPlaybackState() {
        this(0);
    }

    public EffectPlaybackState(int ageTicks) {
        this.ageTicks = ageTicks;
    }

    public int ageTicks() {
        return ageTicks;
    }

    public int lastRuntimeExecutionAge() {
        return lastRuntimeExecutionAge;
    }

    public void tick() {
        ageTicks++;
    }

    public void setAgeTicks(int ageTicks) {
        this.ageTicks = ageTicks;
    }

    public void setLastRuntimeExecutionAge(int lastRuntimeExecutionAge) {
        this.lastRuntimeExecutionAge = lastRuntimeExecutionAge;
    }
}
