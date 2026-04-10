package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.mobs.query.MobClientCoexistenceView;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only client boundary for the minimal synced coexistence projection.
 *
 * <p>This reader only exposes the already-synced new-source view when present. It does not
 * resolve legacy fallbacks, infer gameplay semantics, or act as a second runtime authority.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MobClientCoexistenceReader {

    private MobClientCoexistenceReader() {
    }

    public static Optional<MobClientCoexistenceView> get(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entity instanceof Player) {
            return Optional.empty();
        }
        return MobClientCoexistenceCache.get(entity.getId());
    }

    public static boolean hasEntityId(int entityId) {
        return MobClientCoexistenceCache.get(entityId).isPresent();
    }
}
