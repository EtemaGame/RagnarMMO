package com.etema.ragnarmmo.client.effects;

public record EffectColor(float r, float g, float b, float a) {
    public static final EffectColor WHITE = new EffectColor(1.0f, 1.0f, 1.0f, 1.0f);

    public EffectColor multiply(EffectColor other) {
        return new EffectColor(r * other.r, g * other.g, b * other.b, a * other.a);
    }
}
