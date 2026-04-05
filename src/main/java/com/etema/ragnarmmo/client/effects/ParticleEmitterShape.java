package com.etema.ragnarmmo.client.effects;

import java.util.Locale;

public enum ParticleEmitterShape {
    POINT,
    RING,
    SPHERE;

    public static ParticleEmitterShape parse(String raw) {
        return valueOf(raw.toUpperCase(Locale.ROOT));
    }
}
