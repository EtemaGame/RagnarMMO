package com.etema.ragnarmmo.player.party;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService;
import com.etema.ragnarmmo.player.progression.PlayerProgressionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.etema.ragnarmmo.common.config.RagnarConfigs;

import java.util.List;

/**
 * Service for handling XP sharing within parties.
 * XP is distributed to all eligible members based on party size.
 */
public class PartyXpService {

    /**
     * XP share factors based on party size.
     * Index = number of eligible members (1-6)
     * Following Ragnarok Online's 'Even Share' formula:
     * TotalXP = BaseXP * (1 + 0.2 * (n-1))
     * ShareXP = TotalXP / n
     */
    private static final double[] XP_FACTORS = {
            1.00, // 0 (unused default)
            1.00, // 1 member (solo)
            0.60, // 2 members (120% / 2)
            0.47, // 3 members (140% / 3)
            0.40, // 4 members (160% / 4)
            0.36, // 5 members (180% / 5)
            0.33  // 6 members (200% / 6)
    };

    /**
     * Distributes XP from a mob kill to all eligible party members.
     *
     * @param killer  The player who killed the mob
     * @param baseExp The base XP amount before party modifiers
     * @param server  The server instance
     * @return The XP actually given to the killer (after party modifier)
     */
    public static int distributeKillXp(ServerPlayer killer, int baseExp, MinecraftServer server) {
        if (killer == null || baseExp <= 0 || server == null) {
            return baseExp;
        }

        PartySavedData data = PartySavedData.get(server);
        Party party = data.getPartyByPlayer(killer.getUUID());

        // No party or XP sharing disabled - return full XP to killer
        if (party == null || !party.getSettings().isXpShareEnabled()) {
            return baseExp;
        }

        // Get eligible members (online, in range, same dimension)
        List<ServerPlayer> eligibleMembers = party.getEligibleMembersForXp(killer);
        int memberCount = eligibleMembers.size();

        // Solo in party or only killer eligible - return full XP
        if (memberCount <= 1) {
            return baseExp;
        }

        // Calculate shared XP
        double factor = getXpFactor(memberCount);
        int sharedXp = (int) Math.round(baseExp * factor);
        sharedXp = Math.max(1, sharedXp); // At least 1 XP

        // Distribute to all eligible members
        for (ServerPlayer member : eligibleMembers) {
            if (member.getUUID().equals(killer.getUUID())) {
                // Killer gets their share through the normal flow
                continue;
            }

            // Give XP to party member
            giveXpToMember(member, sharedXp, killer.getName().getString());
        }

        RagnarMMO.LOGGER.debug("Party XP: {} base -> {} shared to {} members (factor {})",
                baseExp, sharedXp, memberCount, factor);

        // Return the modified XP for the killer
        return sharedXp;
    }

    /**
     * Gives XP to a party member (not the killer).
     */
    private static void giveXpToMember(ServerPlayer member, int xp, String killerName) {
        RagnarCoreAPI.get(member).ifPresent(stats -> {
            int pointsPerLevel = RagnarConfigs.SERVER.progression.pointsPerLevel.get();
            PlayerProgressionService progressionService = PlayerProgressionService
                    .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()));
            int baseAward = progressionService.applyBaseExpRate(xp);
            int jobAward = progressionService.applyJobExpRate(xp);

            int levelsGained = stats.addExpAndProcessLevelUps(baseAward, pointsPerLevel, progressionService::baseExpToNext);
            int jobLevelsGained = stats.addJobExpAndProcessLevelUps(jobAward, progressionService::jobExpToNext);

            // Notify member
            member.sendSystemMessage(Component.translatable("party.xp.shared", baseAward, killerName));

            if (levelsGained > 0) {
                member.sendSystemMessage(Component.translatable("message.ragnarmmo.level_up", levelsGained));
            }
            if (jobLevelsGained > 0) {
                member.sendSystemMessage(Component.translatable("message.ragnarmmo.job_level_up", jobLevelsGained));
            }

            PlayerStatsSyncService.sync(member, stats);

            // Send party member update to all party members for HUD
            updatePartyMemberHud(member);
        });
    }

    /**
     * Gets the XP factor for a given number of eligible members.
     */
    public static double getXpFactor(int memberCount) {
        if (memberCount <= 0)
            return 1.0;
        if (memberCount >= XP_FACTORS.length)
            return XP_FACTORS[XP_FACTORS.length - 1];
        return XP_FACTORS[memberCount];
    }

    /**
     * Sends a party member update to all party members.
     * Called when a member's stats change (HP, XP, level).
     */
    public static void updatePartyMemberHud(ServerPlayer player) {
        PartyMemberSyncService.syncCurrent(player);
    }

    /**
     * Called when a player's health changes significantly.
     * Throttled per-player to avoid spam.
     */
    private static final java.util.Map<java.util.UUID, Long> lastHealthUpdateTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long HEALTH_UPDATE_THROTTLE_MS = 500;

    public static void onPlayerHealthChange(ServerPlayer player) {
        if (player == null)
            return;

        java.util.UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastUpdate = lastHealthUpdateTime.get(uuid);

        if (lastUpdate != null && now - lastUpdate < HEALTH_UPDATE_THROTTLE_MS) {
            return;
        }
        lastHealthUpdateTime.put(uuid, now);

        PartyMemberSyncService.syncCurrentIfChanged(player);
    }

    /**
     * Cleanup stale entries when player disconnects.
     */
    public static void clearPlayerThrottle(java.util.UUID uuid) {
        lastHealthUpdateTime.remove(uuid);
    }
}
