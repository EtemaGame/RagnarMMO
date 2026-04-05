package com.etema.ragnarmmo.client.effects.runtime;

import com.etema.ragnarmmo.client.effects.EffectColor;
import com.etema.ragnarmmo.client.effects.EffectVec3;

public final class EffectContext {
    private final float scaleMultiplier;
    private final EffectColor tint;
    private final int durationOverrideTicks;
    private final Float yawOverride;
    private final Float pitchOverride;
    private final EffectVec3 offset;
    private final EffectVec3 normal;

    private EffectContext(Builder builder) {
        this.scaleMultiplier = builder.scaleMultiplier;
        this.tint = builder.tint;
        this.durationOverrideTicks = builder.durationOverrideTicks;
        this.yawOverride = builder.yawOverride;
        this.pitchOverride = builder.pitchOverride;
        this.offset = builder.offset;
        this.normal = builder.normal;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float scaleMultiplier() {
        return scaleMultiplier;
    }

    public EffectColor tint() {
        return tint;
    }

    public int durationOverrideTicks() {
        return durationOverrideTicks;
    }

    public Float yawOverride() {
        return yawOverride;
    }

    public Float pitchOverride() {
        return pitchOverride;
    }

    public EffectVec3 offset() {
        return offset;
    }

    public EffectVec3 normal() {
        return normal;
    }

    public static final class Builder {
        private float scaleMultiplier = 1.0f;
        private EffectColor tint = EffectColor.WHITE;
        private int durationOverrideTicks = -1;
        private Float yawOverride;
        private Float pitchOverride;
        private EffectVec3 offset = EffectVec3.ZERO;
        private EffectVec3 normal = EffectVec3.UP;

        public Builder scaleMultiplier(float scaleMultiplier) {
            this.scaleMultiplier = scaleMultiplier;
            return this;
        }

        public Builder tint(EffectColor tint) {
            this.tint = tint;
            return this;
        }

        public Builder durationOverrideTicks(int durationOverrideTicks) {
            this.durationOverrideTicks = durationOverrideTicks;
            return this;
        }

        public Builder yawOverride(Float yawOverride) {
            this.yawOverride = yawOverride;
            return this;
        }

        public Builder pitchOverride(Float pitchOverride) {
            this.pitchOverride = pitchOverride;
            return this;
        }

        public Builder offset(EffectVec3 offset) {
            this.offset = offset;
            return this;
        }

        public Builder normal(EffectVec3 normal) {
            this.normal = normal;
            return this;
        }

        public EffectContext build() {
            return new EffectContext(this);
        }
    }
}
