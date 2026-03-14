package com.etema.ragnarmmo.roitems.network;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.data.RoItemRuleSet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server-to-client packet that synchronizes all RO item rules.
 * Sent when a player joins and when datapacks are reloaded.
 */
public class SyncRoItemRulesPacket {

    private final Map<ResourceLocation, RoItemRule> itemRules;
    private final Map<ResourceLocation, RoItemRule> tagRules;

    public SyncRoItemRulesPacket(RoItemRuleSet ruleSet) {
        this.itemRules = new HashMap<>();
        this.tagRules = new HashMap<>();

        // Copy item rules
        // We need to iterate through the rule set - but RoItemRuleSet doesn't expose byItemId directly
        // We'll need to add a method or work around this
        // For now, let's serialize what we can access
    }

    public SyncRoItemRulesPacket(Map<ResourceLocation, RoItemRule> itemRules, Map<ResourceLocation, RoItemRule> tagRules) {
        this.itemRules = itemRules;
        this.tagRules = tagRules;
    }

    public static void encode(SyncRoItemRulesPacket msg, FriendlyByteBuf buf) {
        // Write item rules
        buf.writeVarInt(msg.itemRules.size());
        for (var entry : msg.itemRules.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            encodeRule(buf, entry.getValue());
        }

        // Write tag rules
        buf.writeVarInt(msg.tagRules.size());
        for (var entry : msg.tagRules.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            encodeRule(buf, entry.getValue());
        }
    }

    private static void encodeRule(FriendlyByteBuf buf, RoItemRule rule) {
        // displayName (nullable)
        buf.writeBoolean(rule.displayName() != null);
        if (rule.displayName() != null) {
            buf.writeUtf(rule.displayName());
        }

        // requiredBaseLevel
        buf.writeVarInt(rule.requiredBaseLevel());

        // cardSlots
        buf.writeVarInt(rule.cardSlots());

        // attributeBonuses
        Map<StatKeys, Integer> bonuses = rule.attributeBonuses();
        buf.writeVarInt(bonuses.size());
        for (var entry : bonuses.entrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // allowedJobs
        Set<JobType> jobs = rule.allowedJobs();
        buf.writeVarInt(jobs.size());
        for (JobType job : jobs) {
            buf.writeEnum(job);
        }
    }

    public static SyncRoItemRulesPacket decode(FriendlyByteBuf buf) {
        // Read item rules
        int itemCount = buf.readVarInt();
        Map<ResourceLocation, RoItemRule> itemRules = new HashMap<>();
        for (int i = 0; i < itemCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            RoItemRule rule = decodeRule(buf);
            itemRules.put(id, rule);
        }

        // Read tag rules
        int tagCount = buf.readVarInt();
        Map<ResourceLocation, RoItemRule> tagRules = new HashMap<>();
        for (int i = 0; i < tagCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            RoItemRule rule = decodeRule(buf);
            tagRules.put(id, rule);
        }

        return new SyncRoItemRulesPacket(itemRules, tagRules);
    }

    private static RoItemRule decodeRule(FriendlyByteBuf buf) {
        // displayName
        String displayName = null;
        if (buf.readBoolean()) {
            displayName = buf.readUtf();
        }

        // requiredBaseLevel
        int requiredBaseLevel = buf.readVarInt();

        // cardSlots
        int cardSlots = buf.readVarInt();

        // attributeBonuses
        int bonusCount = buf.readVarInt();
        Map<StatKeys, Integer> bonuses = new EnumMap<>(StatKeys.class);
        for (int i = 0; i < bonusCount; i++) {
            StatKeys key = buf.readEnum(StatKeys.class);
            int value = buf.readVarInt();
            bonuses.put(key, value);
        }

        // allowedJobs
        int jobCount = buf.readVarInt();
        Set<JobType> jobs = EnumSet.noneOf(JobType.class);
        for (int i = 0; i < jobCount; i++) {
            jobs.add(buf.readEnum(JobType.class));
        }

        return new RoItemRule(displayName, bonuses, requiredBaseLevel, jobs, cardSlots);
    }

    public static void handle(SyncRoItemRulesPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleRoItemRulesSync(
                        msg.itemRules, msg.tagRules)));
        ctx.setPacketHandled(true);
    }

    public Map<ResourceLocation, RoItemRule> getItemRules() {
        return itemRules;
    }

    public Map<ResourceLocation, RoItemRule> getTagRules() {
        return tagRules;
    }
}
