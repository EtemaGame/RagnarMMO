package com.etema.ragnarmmo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NetworkPacketContractSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");
    private static final Pattern PACKET_CLASS = Pattern.compile("messageBuilder\\(([^.,)]+)\\.class");

    @Test
    void packetRegistrationOrderMatchesBaselineBeforeRangeMigration() throws IOException {
        List<String> packets = new ArrayList<>();
        packets.addAll(packetClasses("items/network/RoItemsNetwork.java"));
        packets.addAll(packetClasses("achievements/network/AchievementNetwork.java"));
        packets.addAll(packetClasses("common/net/effects/SkillEffectsNetwork.java"));
        packets.addAll(packetClasses("combat/net/CombatNetwork.java"));
        packets.addAll(packetClasses("player/stats/network/StatsNetwork.java"));
        packets.addAll(packetClasses("skills/net/SkillsNetwork.java"));
        packets.addAll(packetClasses("player/party/net/PartyNetwork.java"));
        packets.addAll(packetClasses("mobs/network/MobNetwork.java"));
        packets.addAll(packetClasses("lifeskills/LifeSkillsNetwork.java"));
        packets.addAll(packetClasses("economy/zeny/network/EconomyNetwork.java"));
        packets.addAll(packetClasses("bestiary/network/BestiaryNetwork.java"));

        assertEquals(List.of(
                "SyncRoItemRulesPacket",
                "CardCompoundPacket",
                "SyncAchievementsPacket",
                "ClaimAchievementPacket",
                "SetTitlePacket",
                "SyncAchievementDefinitionsPacket",
                "SkillPhaseWorldEffectPacket",
                "ServerboundRagnarBasicAttackPacket",
                "ServerboundRagnarSkillUsePacket",
                "ClientboundRagnarCombatResultPacket",
                "ClientboundRagnarCastStatePacket",
                "PlayerStatsSyncPacket",
                "AllocateStatPacket",
                "DeallocateStatPacket",
                "ClientboundSkillXpPacket",
                "ClientboundSkillSyncPacket",
                "ClientboundLevelUpPacket",
                "PacketChangeJob",
                "PacketResetCharacter",
                "PacketUpgradeSkill",
                "DerivedStatsSyncPacket",
                "PacketUseSkill",
                "PacketSetHotbarSlot",
                "ClientboundCastUpdatePacket",
                "SyncSkillDefinitionsPacket",
                "SyncSkillTreesPacket",
                "PartySnapshotS2CPacket",
                "PartyMemberUpdateS2CPacket",
                "SyncMobProfilePacket",
                "MobHurtPacket",
                "LifeSkillSyncPacket",
                "LifeSkillUpdatePacket",
                "LifeSkillPointsPacket",
                "LifeSkillLevelUpPacket",
                "LifeSkillPerkChoicePacket",
                "WalletSyncPacket",
                "ZenyBagActionPacket",
                "SyncBestiaryIndexPacket",
                "RequestBestiaryIndexPacket",
                "SyncBestiaryDetailsPacket",
                "RequestBestiaryDetailsPacket"
        ), packets);
    }

    @Test
    void moduleOwnershipKeepsGameplaySystemsOutOfStatsModule() throws IOException {
        String stats = Files.readString(ROOT.resolve("common/init/modules/StatsModule.java"));
        String skills = Files.readString(ROOT.resolve("common/init/modules/SkillsModule.java"));
        String party = Files.readString(ROOT.resolve("common/init/modules/PartyModule.java"));
        String items = Files.readString(ROOT.resolve("items/ItemsModule.java"));
        String main = Files.readString(ROOT.resolve("RagnarMMO.java"));

        assertFalse(stats.contains("registerSkillPackets"));
        assertFalse(stats.contains("registerPartyPackets"));
        assertTrue(skills.contains("Network.registerSkillPackets();"));
        assertTrue(party.contains("Network.registerPartyPackets();"));
        assertTrue(items.contains("RagnarLootModifiers.register(modBus);"));
        assertTrue(main.indexOf("StatsModule.init(modBus);") < main.indexOf("SkillsModule.init(modBus);"));
        assertTrue(main.indexOf("SkillsModule.init(modBus);") < main.indexOf("PartyModule.init(modBus);"));
        assertTrue(main.indexOf("PartyModule.init(modBus);") < main.indexOf("MobsModule.init(modBus);"));
    }

    @Test
    void packetRangePlanIsDocumentedButNotAppliedYet() throws IOException {
        String network = Files.readString(ROOT.resolve("common/net/Network.java"));

        assertTrue(network.contains("Registration order is preserved to maintain stable packet IDs."));
        assertTrue(network.contains("core/stats 000-099"));
        assertTrue(network.contains("combat 100-199"));
        assertTrue(network.contains("skills 200-299"));
        assertTrue(network.contains("party 300-399"));
        assertTrue(network.contains("mobs 400-499"));
        assertTrue(network.contains("items 500-599"));
        assertTrue(network.contains("economy 600-699"));
        assertTrue(network.contains("bestiary 700-799"));
    }

    private static List<String> packetClasses(String relativePath) throws IOException {
        String source = Files.readString(ROOT.resolve(relativePath));
        var matcher = PACKET_CLASS.matcher(source);
        List<String> packets = new ArrayList<>();
        while (matcher.find()) {
            packets.add(matcher.group(1));
        }
        return packets;
    }
}
