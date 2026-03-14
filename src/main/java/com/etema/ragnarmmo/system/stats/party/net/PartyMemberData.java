package com.etema.ragnarmmo.system.stats.party.net;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Network-serializable data for a party member.
 * Used by both PartySnapshotS2CPacket and PartyMemberUpdateS2CPacket.
 */
public record PartyMemberData(
    UUID uuid,
    String name,
    float currentHealth,
    float maxHealth,
    int level,
    int exp,
    int expToNextLevel,
    boolean isLeader,
    boolean isOnline
) {

    /**
     * Creates member data from a server player.
     */
    public static PartyMemberData fromPlayer(ServerPlayer player, boolean isLeader) {
        if (player == null) {
            return null;
        }

        // Get level data from RagnarMMO stats
        int level = 1;
        int exp = 0;
        int expToNext = 100;

        var stats = RagnarCoreAPI.get(player);
        if (stats.isPresent()) {
            var s = stats.get();
            level = s.getLevel();
            exp = s.getExp();
            expToNext = com.etema.ragnarmmo.system.stats.progression.ExpTable.expToNext(level);
        }

        return new PartyMemberData(
            player.getUUID(),
            player.getName().getString(),
            player.getHealth(),
            player.getMaxHealth(),
            level,
            exp,
            expToNext,
            isLeader,
            true
        );
    }

    /**
     * Creates offline member data (minimal info).
     */
    public static PartyMemberData offline(UUID uuid, boolean isLeader) {
        return new PartyMemberData(
            uuid,
            uuid.toString().substring(0, 8),
            0f,
            20f,
            1,
            0,
            100,
            isLeader,
            false
        );
    }

    /**
     * Encodes this data to a network buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(name, 64);
        buf.writeFloat(currentHealth);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(level);
        buf.writeVarInt(exp);
        buf.writeVarInt(expToNextLevel);
        buf.writeBoolean(isLeader);
        buf.writeBoolean(isOnline);
    }

    /**
     * Decodes member data from a network buffer.
     */
    public static PartyMemberData decode(FriendlyByteBuf buf) {
        return new PartyMemberData(
            buf.readUUID(),
            buf.readUtf(64),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean()
        );
    }

    /**
     * Gets the experience progress as a percentage (0.0 - 1.0).
     */
    public float getExpProgress() {
        if (expToNextLevel <= 0) return 0f;
        return Math.min(1f, (float) exp / expToNextLevel);
    }

    /**
     * Gets the health progress as a percentage (0.0 - 1.0).
     */
    public float getHealthProgress() {
        if (maxHealth <= 0) return 0f;
        return Math.min(1f, currentHealth / maxHealth);
    }
}
