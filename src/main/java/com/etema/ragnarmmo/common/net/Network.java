package com.etema.ragnarmmo.common.net;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.player.stats.network.StatsNetwork;
import com.etema.ragnarmmo.player.party.net.PartyNetwork;
import com.etema.ragnarmmo.mobs.network.MobNetwork;
import com.etema.ragnarmmo.lifeskills.LifeSkillsNetwork;
import com.etema.ragnarmmo.items.network.RoItemsNetwork;
import com.etema.ragnarmmo.system.achievements.network.AchievementNetwork;
import com.etema.ragnarmmo.common.net.effects.SkillEffectsNetwork;
import com.etema.ragnarmmo.combat.net.CombatNetwork;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified networking channel for RagnarMMO.
 * <p>
 * The channel is shared across all modules, but each module registers its own
 * packets via dedicated {@code *Network} classes colocated with their packets.
 * <p>
 * Registration order is preserved to maintain stable packet IDs.
 */
@SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
public final class Network {
        private static final String PROTOCOL = "2";
        private static final SimpleChannel CH = NetworkRegistry.ChannelBuilder
                        .named(new ResourceLocation(RagnarMMO.MODID, "main"))
                        .networkProtocolVersion(() -> PROTOCOL)
                        .clientAcceptedVersions(PROTOCOL::equals)
                        .serverAcceptedVersions(PROTOCOL::equals)
                        .simpleChannel();

        private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

        private Network() {
        }

        public static SimpleChannel channel() {
                return CH;
        }

        /* ─── Per-module registration (call order = packet ID order) ─── */

        public static void registerStatsPackets() {
                StatsNetwork.register(CH, NEXT_ID);
        }

        public static void registerPartyPackets() {
                PartyNetwork.register(CH, NEXT_ID);
        }

        public static void registerMobPackets() {
                MobNetwork.register(CH, NEXT_ID);
        }

        public static void registerRoItemPackets() {
                RoItemsNetwork.register(CH, NEXT_ID);
        }

        public static void registerLifeSkillPackets() {
                LifeSkillsNetwork.register(CH, NEXT_ID);
        }

        public static void registerAchievementPackets() {
                AchievementNetwork.register(CH, NEXT_ID);
        }

        public static void registerSkillEffectPackets() {
                SkillEffectsNetwork.register(CH, NEXT_ID);
        }

        public static void registerCombatPackets() {
                CombatNetwork.register(CH, NEXT_ID);
        }

        public static void registerEconomyPackets() {
                com.etema.ragnarmmo.economy.zeny.network.EconomyNetwork.register(CH, NEXT_ID);
        }

        /* ─── Helpers ─── */

        public static <T> void sendToPlayer(ServerPlayer player, T msg) {
                if (player.connection == null) {
                        return;
                }
                CH.send(PacketDistributor.PLAYER.with(() -> player), msg);
        }

        public static <T> void sendToServer(T msg) {
                CH.sendToServer(msg);
        }

        public static <T> void sendTrackingEntityAndSelf(Entity entity, T msg) {
                CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
        }

        public static <T> void sendToTrackingEntity(Entity entity, T msg) {
                CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
        }
}
