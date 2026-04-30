package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PacketFirstBasicAttackAuthoritySourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void attackEntityEventIsCancelFirstAndFallbackIsConfigGated() throws IOException {
        String handler = read("combat/event/BasicAttackEventHandler.java");
        String configs = read("common/config/RagnarConfigs.java");

        assertTrue(handler.contains("EventPriority.HIGHEST"),
                "AttackEntityEvent should cancel vanilla melee before internal compat handlers");
        assertTrue(handler.contains("event.setCanceled(true)"),
                "AttackEntityEvent must cancel vanilla melee");
        assertTrue(handler.contains("serverEventFallbackEnabled"),
                "SERVER_ATTACK_EVENT fallback must be config-gated");
        assertTrue(configs.contains("basic_attack_server_event_fallback_enabled\", false"),
                "SERVER_ATTACK_EVENT fallback must default to false");
    }

    @Test
    void basicAttackUsesSeparatedSequenceStateAndTransactionalCommit() throws IOException {
        String state = read("combat/state/CombatActorState.java");
        String engine = read("combat/engine/RagnarCombatEngine.java");

        assertTrue(state.contains("lastClientPacketSequenceId"));
        assertTrue(state.contains("lastServerFallbackTick"));
        assertTrue(state.contains("lastObservedPacketIntent"));
        assertTrue(engine.contains("commitAcceptedBasicAttack("),
                "Cooldown and sequence updates must be centralized");
        assertTrue(engine.contains("setLastClientPacketSequenceId(sequenceId)"));
        assertTrue(engine.contains("setLastServerFallbackTick(nowTick)"));
    }

    @Test
    void contractRejectionIsNotEncodedAsMiss() throws IOException {
        String engine = read("combat/engine/RagnarCombatEngine.java");

        assertTrue(engine.contains("BasicHitPreparation.InfrastructureFailure"));
        assertTrue(engine.contains("BasicAttackFailureReason.CONTRACT_REJECTED"));
        assertFalse(engine.contains("COMBAT_CONTRACT_REJECTED_\" + result.rejectReason());\n            return CombatResolution.miss"),
                "Contract rejection must not become a fake RO MISS");
    }

    @Test
    void lazyMobProfileInitPreservesHealthRatio() throws IOException {
        String bootstrap = read("mobs/spawn/MobProfileBootstrap.java");
        String resolver = read("combat/contract/CombatantProfileResolver.java");
        String attributes = read("mobs/util/MobAttributeHelper.java");

        assertTrue(resolver.contains("MobProfileBootstrap.ensureInitialized"));
        assertTrue(bootstrap.contains("COMBAT_LAZY"));
        assertTrue(bootstrap.contains("HealthPreservationMode.PRESERVE_RATIO"),
                "Combat lazy init must not full-heal mobs");
        assertFalse(attributes.contains("case PRESERVE_RATIO -> {\n                    if (mob.tickCount < 10"),
                "PRESERVE_RATIO must preserve ratio even for freshly spawned mobs");
    }

    @Test
    void commonEventsDoesNotRecalculateUnmarkedMeleeWhenContractAuthorityIsActive() throws IOException {
        String commonEvents = read("player/stats/event/CommonEvents.java");
        String guard = read("common/util/DamageProcessingGuard.java");

        assertTrue(commonEvents.contains("serverEventFallbackEnabled"));
        assertTrue(commonEvents.contains("e.setCanceled(true)"));
        assertTrue(guard.contains("markBasicAttack"));
        assertTrue(guard.contains("markSkillContractDamage"));
        assertTrue(guard.contains("markRangedSnapshot"));
        assertTrue(guard.contains("markMobToPlayerContract"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
