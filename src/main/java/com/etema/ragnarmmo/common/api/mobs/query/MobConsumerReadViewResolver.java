package com.etema.ragnarmmo.common.api.mobs.query;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.common.api.mobs.LegacyMobTierRankMapper;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.api.mobs.runtime.projection.ComputedMobProfileReadView;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Central source selector for the shared mob consumer read surface.
 *
 * <p>This resolver prefers the new {@link ComputedMobProfile} when present and otherwise falls back
 * to a controlled legacy compatibility projection. It does not migrate any concrete consumer by itself.</p>
 */
public final class MobConsumerReadViewResolver {

    private MobConsumerReadViewResolver() {
    }

    public static Optional<MobConsumerReadView> resolve(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        ComputedMobProfile computedProfile = ManualMobProfileRuntimeStore.get(entity).orElse(null);
        if (computedProfile != null) {
            return resolve(entity, computedProfile, null);
        }

        return resolve(entity, null, MobStatsProvider.get(entity).orElse(null));
    }

    public static Optional<MobConsumerReadView> resolve(
            LivingEntity entity,
            @Nullable MobStats legacyStats) {
        return resolve(entity, null, legacyStats);
    }

    public static Optional<MobConsumerReadView> resolve(
            LivingEntity entity,
            @Nullable ComputedMobProfile computedProfile,
            @Nullable MobStats legacyStats) {
        Objects.requireNonNull(entity, "entity");

        ComputedMobProfile effectiveProfile = computedProfile != null
                ? computedProfile
                : ManualMobProfileRuntimeStore.get(entity).orElse(null);

        if (effectiveProfile != null) {
            return Optional.of(fromComputedProfile(entity, effectiveProfile));
        }
        if (legacyStats != null) {
            return fromLegacyCompatibility(entity, legacyStats);
        }
        return Optional.empty();
    }

    private static MobConsumerReadView fromComputedProfile(
            LivingEntity entity,
            ComputedMobProfile computedProfile) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        ComputedMobProfileReadView readView = ComputedMobProfileReadView.from(computedProfile);
        boolean bossLikeForCompatibility = readView.isBossLikeForCompatibility()
                || MobSpawnOverrides.isManualBoss(entity);

        return new MobConsumerReadView(
                entityTypeId,
                MobConsumerDataOrigin.NEW_RUNTIME_PROFILE,
                readView.level(),
                readView.rank(),
                readView.race(),
                readView.element(),
                readView.size(),
                bossLikeForCompatibility,
                true,
                new MobConsumerInspectionStatsView(
                        computedProfile.maxHp(),
                        computedProfile.atkMin(),
                        computedProfile.atkMax(),
                        computedProfile.def(),
                        computedProfile.mdef()));
    }

    private static Optional<MobConsumerReadView> fromLegacyCompatibility(
            LivingEntity entity,
            MobStats legacyStats) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        MobRank legacyRank = LegacyMobTierRankMapper.toMobRank(legacyStats.getTier());
        String race = CombatPropertyResolver.getRaceId(entity);
        String size = CombatPropertyResolver.getSizeId(entity);
        String element = CombatPropertyResolver.getElementId(legacyStats.getElement());
        boolean bossLikeForCompatibility = legacyRank == MobRank.BOSS
                || MobSpawnOverrides.isManualBoss(entity);

        if (race.isBlank() || size.isBlank() || element.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new MobConsumerReadView(
                entityTypeId,
                MobConsumerDataOrigin.LEGACY_COMPATIBILITY,
                legacyStats.getLevel(),
                legacyRank,
                race,
                element,
                size,
                bossLikeForCompatibility,
                false,
                null));
    }
}
