package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.mobs.query.MobClientCoexistenceView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side storage for the minimal new-source coexistence projection keyed by entity id.
 */
@OnlyIn(Dist.CLIENT)
public final class MobClientCoexistenceCache {

    private static final Map<Integer, MobClientCoexistenceView> VIEWS = new ConcurrentHashMap<>();

    private MobClientCoexistenceCache() {
    }

    public static void put(int entityId, MobClientCoexistenceView view) {
        VIEWS.put(entityId, view);
    }

    public static Optional<MobClientCoexistenceView> get(int entityId) {
        return Optional.ofNullable(VIEWS.get(entityId));
    }

    public static void remove(int entityId) {
        VIEWS.remove(entityId);
    }

    public static void clear() {
        VIEWS.clear();
    }
}
