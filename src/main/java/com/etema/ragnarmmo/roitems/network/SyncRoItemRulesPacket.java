package com.etema.ragnarmmo.roitems.network;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.roitems.data.RoCombatProfile;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.data.RoItemRuleSet;
import com.etema.ragnarmmo.system.loot.cards.CardEquipType;

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
    private final Map<String, Map<CardEquipType, RoItemRule>> modTypeRules;
    private final Map<CardEquipType, RoItemRule> fallbackRules;

    public SyncRoItemRulesPacket(RoItemRuleSet ruleSet) {
        this(ruleSet.getItemRules(), ruleSet.getTagRules(), ruleSet.getModTypeRules(), ruleSet.getFallbackRules());
    }

    public SyncRoItemRulesPacket(Map<ResourceLocation, RoItemRule> itemRules,
                                 Map<ResourceLocation, RoItemRule> tagRules,
                                 Map<String, Map<CardEquipType, RoItemRule>> modTypeRules,
                                 Map<CardEquipType, RoItemRule> fallbackRules) {
        this.itemRules = new HashMap<>(itemRules);
        this.tagRules = new HashMap<>(tagRules);
        this.modTypeRules = new HashMap<>();
        modTypeRules.forEach((modId, rules) -> {
            Map<CardEquipType, RoItemRule> byType = new EnumMap<>(CardEquipType.class);
            byType.putAll(rules);
            this.modTypeRules.put(modId, byType);
        });
        this.fallbackRules = new EnumMap<>(CardEquipType.class);
        this.fallbackRules.putAll(fallbackRules);
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

        buf.writeVarInt(msg.modTypeRules.size());
        for (var modEntry : msg.modTypeRules.entrySet()) {
            buf.writeUtf(modEntry.getKey());
            buf.writeVarInt(modEntry.getValue().size());
            for (var typeEntry : modEntry.getValue().entrySet()) {
                buf.writeEnum(typeEntry.getKey());
                encodeRule(buf, typeEntry.getValue());
            }
        }

        buf.writeVarInt(msg.fallbackRules.size());
        for (var entry : msg.fallbackRules.entrySet()) {
            buf.writeEnum(entry.getKey());
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

        // showTooltip
        buf.writeBoolean(rule.showTooltip());

        encodeCombatProfile(buf, rule.combatProfile());

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

    private static void encodeCombatProfile(FriendlyByteBuf buf, RoCombatProfile profile) {
        RoCombatProfile combatProfile = profile != null ? profile : RoCombatProfile.EMPTY;
        buf.writeEnum(combatProfile.weaponMode());
        buf.writeDouble(combatProfile.atk());
        buf.writeDouble(combatProfile.matk());
        buf.writeVarInt(combatProfile.aspd());
        buf.writeDouble(combatProfile.range());
        buf.writeVarInt(combatProfile.drawTicks());
        buf.writeFloat(combatProfile.projectileVelocity());
        writeStringSet(buf, combatProfile.atkAttributeIds());
        writeStringSet(buf, combatProfile.matkAttributeIds());
        writeStringSet(buf, combatProfile.aspdAttributeIds());
        writeStringSet(buf, combatProfile.rangeAttributeIds());
    }

    private static void writeStringSet(FriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
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

        int modCount = buf.readVarInt();
        Map<String, Map<CardEquipType, RoItemRule>> modTypeRules = new HashMap<>();
        for (int i = 0; i < modCount; i++) {
            String modId = buf.readUtf();
            int typeCount = buf.readVarInt();
            Map<CardEquipType, RoItemRule> byType = new EnumMap<>(CardEquipType.class);
            for (int j = 0; j < typeCount; j++) {
                CardEquipType equipType = buf.readEnum(CardEquipType.class);
                byType.put(equipType, decodeRule(buf));
            }
            modTypeRules.put(modId, byType);
        }

        int fallbackCount = buf.readVarInt();
        Map<CardEquipType, RoItemRule> fallbackRules = new EnumMap<>(CardEquipType.class);
        for (int i = 0; i < fallbackCount; i++) {
            CardEquipType equipType = buf.readEnum(CardEquipType.class);
            fallbackRules.put(equipType, decodeRule(buf));
        }

        return new SyncRoItemRulesPacket(itemRules, tagRules, modTypeRules, fallbackRules);
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

        boolean showTooltip = buf.readBoolean();
        RoCombatProfile combatProfile = decodeCombatProfile(buf);

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

        return new RoItemRule(displayName, bonuses, requiredBaseLevel, jobs, cardSlots, showTooltip, combatProfile);
    }

    private static RoCombatProfile decodeCombatProfile(FriendlyByteBuf buf) {
        return new RoCombatProfile(
                buf.readEnum(RoCombatProfile.WeaponMode.class),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readFloat(),
                readStringSet(buf),
                readStringSet(buf),
                readStringSet(buf),
                readStringSet(buf));
    }

    private static Set<String> readStringSet(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<String> values = new java.util.LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            values.add(buf.readUtf());
        }
        return values;
    }

    public static void handle(SyncRoItemRulesPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleRoItemRulesSync(
                        msg.itemRules, msg.tagRules, msg.modTypeRules, msg.fallbackRules)));
        ctx.setPacketHandled(true);
    }

    public Map<ResourceLocation, RoItemRule> getItemRules() {
        return itemRules;
    }

    public Map<ResourceLocation, RoItemRule> getTagRules() {
        return tagRules;
    }

    public Map<String, Map<CardEquipType, RoItemRule>> getModTypeRules() {
        return modTypeRules;
    }

    public Map<CardEquipType, RoItemRule> getFallbackRules() {
        return fallbackRules;
    }
}
