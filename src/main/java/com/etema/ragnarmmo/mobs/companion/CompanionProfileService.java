package com.etema.ragnarmmo.mobs.companion;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyMode;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyResult;
import com.etema.ragnarmmo.mobs.network.SyncMobProfilePacket;
import com.etema.ragnarmmo.mobs.profile.AuthoredMobProfileResolver;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobProfileFactory;
import com.etema.ragnarmmo.mobs.util.MobAttributeHelper;
import com.etema.ragnarmmo.mobs.util.MobProfileEligibility;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

public final class CompanionProfileService {
    private static final MobProfileFactory PROFILE_FACTORY = new MobProfileFactory();

    private CompanionProfileService() {
    }

    public static void refreshOnJoin(LivingEntity companion, MobProfileState state) {
        Optional<UUID> ownerId = MobProfileEligibility.ownerUuid(companion);
        if (ownerId.isEmpty()) {
            return;
        }

        ServerPlayer owner = resolveOnlineOwner(companion, ownerId.get());
        if (owner == null) {
            handleMissingOwner(companion, state, ownerId.get());
            return;
        }

        refreshFromOwner(companion, state, owner, ownerId.get());
    }

    public static void refreshOwnedCompanions(ServerPlayer owner) {
        if (owner == null || owner.level().isClientSide()) {
            return;
        }
        double radius = MobConfigAccess.getCompanionRules().syncRadius();
        AABB area = owner.getBoundingBox().inflate(radius);
        ServerLevel level = owner.serverLevel();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive()
                        && MobProfileEligibility.isCompanion(entity)
                        && MobProfileEligibility.ownerUuid(entity)
                                .map(owner.getUUID()::equals)
                                .orElse(false))) {
            MobProfileProvider.get(entity).ifPresent(state -> refreshFromOwner(entity, state, owner, owner.getUUID()));
        }
    }

    private static void handleMissingOwner(LivingEntity companion, MobProfileState state, UUID ownerId) {
        if (state.isInitialized()) {
            return;
        }
        if (MobConfigAccess.getCompanionRules().deferUntilOwnerOnline()) {
            return;
        }
        MobProfile profile = createProfile(companion, 1, MobRank.NORMAL);
        state.setProfile(profile);
        MobAttributeHelper.applyAttributes(companion, profile, MobAttributeHelper.HealthPreservationMode.PRESERVE_RATIO);
        Network.sendTrackingEntityAndSelf(companion, new SyncMobProfilePacket(companion.getId(), profile));
    }

    private static void refreshFromOwner(LivingEntity companion, MobProfileState state, ServerPlayer owner, UUID ownerId) {
        RagnarCoreAPI.get(owner).ifPresent(stats -> {
            int level = Math.max(1, stats.getLevel());
            MobRank rank = resolveRank(companion, state, ownerId);
            MobProfile profile = createProfile(companion, level, rank);
            state.setProfile(profile);
            MobAttributeHelper.applyAttributes(companion, profile, MobAttributeHelper.HealthPreservationMode.PRESERVE_RATIO);
            Network.sendTrackingEntityAndSelf(companion, new SyncMobProfilePacket(companion.getId(), profile));
        });
    }

    private static MobProfile createProfile(LivingEntity companion, int level, MobRank rank) {
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(companion.getType());
        DifficultyResult difficulty = new DifficultyResult(
                Math.max(1, level),
                rank,
                Math.max(1, level),
                Math.max(1, level),
                Optional.empty(),
                Optional.empty(),
                DifficultyMode.PLAYER_LEVEL);
        return PROFILE_FACTORY.create(difficulty, AuthoredMobProfileResolver.resolvePartialDefinition(entityTypeId));
    }

    private static MobRank resolveRank(LivingEntity companion, MobProfileState state, UUID ownerId) {
        CompanionRankMode mode = MobConfigAccess.getCompanionRules().rankMode();
        if (mode == CompanionRankMode.NORMAL_ONLY) {
            return MobRank.NORMAL;
        }

        if (state.isInitialized()) {
            MobRank existing = state.profile().rank();
            if (existing == MobRank.NORMAL || existing == MobRank.ELITE) {
                return existing;
            }
        }

        long seed = companion.level() instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        seed = mix(seed, companion.getUUID());
        seed = mix(seed, ownerId);
        return MobConfigAccess.getDifficultyRules().rankChances().roll(RandomSource.create(seed).nextDouble());
    }

    private static ServerPlayer resolveOnlineOwner(LivingEntity companion, UUID ownerId) {
        MinecraftServer server = companion.level().getServer();
        return server == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    private static long mix(long seed, UUID value) {
        if (value == null) {
            return seed;
        }
        seed ^= value.getMostSignificantBits();
        seed = Long.rotateLeft(seed, 21) * 0x9E3779B97F4A7C15L;
        seed ^= value.getLeastSignificantBits();
        return Long.rotateLeft(seed, 17) * 0xC2B2AE3D27D4EB4FL;
    }
}
