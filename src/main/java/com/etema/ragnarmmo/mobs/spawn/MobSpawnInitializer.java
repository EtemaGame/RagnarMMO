package com.etema.ragnarmmo.mobs.spawn;

import com.etema.ragnarmmo.mobs.difficulty.DifficultyContext;
import com.etema.ragnarmmo.mobs.difficulty.MobDifficultyResolver;
import com.etema.ragnarmmo.mobs.profile.AuthoredMobProfileResolver;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobProfileFactory;

public final class MobSpawnInitializer {
    private final MobDifficultyResolver difficultyResolver;
    private final MobProfileFactory profileFactory;

    public MobSpawnInitializer(MobDifficultyResolver difficultyResolver, MobProfileFactory profileFactory) {
        this.difficultyResolver = difficultyResolver;
        this.profileFactory = profileFactory;
    }

    public MobProfile initialize(DifficultyContext context) {
        return profileFactory.create(
                difficultyResolver.resolve(context),
                AuthoredMobProfileResolver.resolvePartialDefinition(context.entityType()));
    }
}
