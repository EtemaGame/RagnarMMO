package com.etema.ragnarmmo.client.effects;

public record EffectVec3(float x, float y, float z) {
    public static final EffectVec3 ZERO = new EffectVec3(0.0f, 0.0f, 0.0f);
    public static final EffectVec3 UP = new EffectVec3(0.0f, 1.0f, 0.0f);
}
