package com.etema.ragnarmmo.mobs.spawn;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.companion.CompanionProfileService;
import com.etema.ragnarmmo.mobs.network.SyncMobProfilePacket;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.util.MobProfileEligibility;
import com.etema.ragnarmmo.mobs.util.MobProfileEligibility.Classification;
import com.etema.ragnarmmo.player.stats.util.AntiFarmManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class MobSpawnHandler {

    private final Random rng = new Random();

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!RagnarConfigs.SERVER.progression.antiFarmSpawnReduction.get()) {
            return;
        }

        Player nearest = event.getLevel().getNearestPlayer(event.getX(), event.getY(), event.getZ(), 64, false);
        if (nearest != null) {
            double penalty = AntiFarmManager.getPenaltyFactor(nearest);
            if (penalty < 1.0D && rng.nextDouble() > penalty) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!MobConfigAccess.isEnabled()) {
            return;
        }

        MobProfileProvider.get(living).ifPresent(state -> {
            Classification classification = MobProfileEligibility.classify(living);
            switch (classification) {
                case INELIGIBLE -> clearLegacyProfile(living, state);
                case STANDARD_MOB -> initializeStandardMob(living, state);
                case COMPANION -> CompanionProfileService.refreshOnJoin(living, state);
            }
        });
    }

    private void clearLegacyProfile(LivingEntity living, MobProfileState state) {
        if (!state.isInitialized()) {
            return;
        }
        state.clearProfile();
        Network.sendTrackingEntityAndSelf(living, SyncMobProfilePacket.clear(living));
    }

    private void initializeStandardMob(LivingEntity living, MobProfileState state) {
        MobProfile profile = initializeCanonicalProfile(living, state);
        RagnarDebugLog.mobSpawns(
                "CANONICAL SPAWN mob={} pos={} rank={} level={}",
                RagnarDebugLog.entityLabel(living),
                RagnarDebugLog.blockPos(living.blockPosition()),
                profile.rank(),
                profile.level());
    }

    private MobProfile initializeCanonicalProfile(LivingEntity living, MobProfileState state) {
        return MobProfileBootstrap.ensureInitialized(living, state, MobProfileBootstrap.InitReason.SPAWN)
                .orElseThrow(() -> new IllegalStateException("Unable to initialize eligible mob profile"));
    }
}
