package com.etema.ragnarmmo.client.effects.runtime;

import com.etema.ragnarmmo.client.effects.SkillEffectDefinition;

import java.util.UUID;

public final class EffectInstance {
    private final UUID id;
    private final SkillEffectDefinition definition;
    private final EffectAnchor anchor;
    private final EffectContext context;
    private final EffectPlaybackState playbackState;

    public EffectInstance(SkillEffectDefinition definition, EffectAnchor anchor, EffectContext context) {
        this(UUID.randomUUID(), definition, anchor, context, new EffectPlaybackState());
    }

    public EffectInstance(UUID id, SkillEffectDefinition definition, EffectAnchor anchor, EffectContext context,
            EffectPlaybackState playbackState) {
        this.id = id;
        this.definition = definition;
        this.anchor = anchor;
        this.context = context;
        this.playbackState = playbackState;
    }

    public UUID id() {
        return id;
    }

    public SkillEffectDefinition definition() {
        return definition;
    }

    public EffectAnchor anchor() {
        return anchor;
    }

    public EffectContext context() {
        return context;
    }

    public EffectPlaybackState playbackState() {
        return playbackState;
    }

    public int durationTicks() {
        return context.durationOverrideTicks() > 0 ? context.durationOverrideTicks() : definition.durationTicks();
    }

    public boolean isExpired() {
        return !definition.loop() && playbackState.ageTicks() >= durationTicks();
    }
}
