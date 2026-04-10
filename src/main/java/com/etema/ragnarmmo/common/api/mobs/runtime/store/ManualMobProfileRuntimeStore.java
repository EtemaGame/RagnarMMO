package com.etema.ragnarmmo.common.api.mobs.runtime.store;

import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Narrow runtime store for the new manual mob profile during coexistence.
 *
 * <p>This store only associates an already-computed {@link ComputedMobProfile} with a live entity
 * instance. It does not compute profiles, apply attributes, or become a second runtime authority.</p>
 */
public final class ManualMobProfileRuntimeStore {

    private static final Map<LivingEntity, ComputedMobProfile> ATTACHED_PROFILES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ManualMobProfileRuntimeStore() {
    }

    public static void attach(LivingEntity entity, ComputedMobProfile profile) {
        Objects.requireNonNull(entity, "entity");
        ATTACHED_PROFILES.put(entity, Objects.requireNonNull(profile, "profile"));
    }

    public static Optional<ComputedMobProfile> get(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(ATTACHED_PROFILES.get(entity));
    }

    public static void clear(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        ATTACHED_PROFILES.remove(entity);
    }
}
