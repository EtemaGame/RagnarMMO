package com.etema.ragnarmmo.skills.net;

import com.etema.ragnarmmo.skills.api.SkillCategory;
import com.etema.ragnarmmo.skills.api.SkillTier;
import com.etema.ragnarmmo.skills.api.SkillUsageType;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillLevelData;
import com.etema.ragnarmmo.skills.data.SkillReference;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SyncSkillDefinitionsPacket {
    private final Collection<SkillDefinition> definitions;

    public SyncSkillDefinitionsPacket(Collection<SkillDefinition> definitions) {
        this.definitions = definitions;
    }

    public SyncSkillDefinitionsPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.definitions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            definitions.add(decodeDefinition(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(definitions.size());
        for (SkillDefinition def : definitions) {
            encodeDefinition(buf, def);
        }
    }

    private void encodeDefinition(FriendlyByteBuf buf, SkillDefinition def) {
        buf.writeResourceLocation(def.getId());
        buf.writeUtf(def.getDisplayName());
        buf.writeEnum(def.getCategory());
        buf.writeEnum(def.getTier());
        buf.writeEnum(def.getUsageType());
        buf.writeUtf(def.getScalingStat());
        buf.writeDouble(def.getXpMultiplier());

        buf.writeVarInt(def.getBaseCost());
        buf.writeVarInt(def.getCostPerLevel());

        buf.writeVarInt(def.getCooldownTicks());
        buf.writeVarInt(def.getCastDelayTicks());
        buf.writeVarInt(def.getCastTimeTicks());
        buf.writeBoolean(def.isInterruptible());

        buf.writeVarInt(def.getMaxLevel());
        buf.writeVarInt(def.getUpgradeCost());
        buf.writeBoolean(def.canGainXp());
        buf.writeBoolean(def.canUpgradeWithPoints());

        // Requirements
        Map<ResourceLocation, Integer> reqs = def.getRequirements();
        buf.writeVarInt(reqs.size());
        reqs.forEach((id, val) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(val);
        });

        // Jobs
        Set<String> jobs = def.getAllowedJobs();
        buf.writeVarInt(jobs.size());
        jobs.forEach(buf::writeUtf);

        // UI
        buf.writeBoolean(def.getIcon() != null);
        if (def.getIcon() != null) buf.writeResourceLocation(def.getIcon());
        buf.writeUtf(def.getTextureName());
        buf.writeVarInt(def.getGridX());
        buf.writeVarInt(def.getGridY());

        // Effect & Reference
        buf.writeBoolean(def.getEffectClass() != null);
        if (def.getEffectClass() != null) buf.writeUtf(def.getEffectClass());

        // Level Data
        Map<Integer, SkillLevelData> levelData = def.getLevelDataMap();
        buf.writeVarInt(levelData.size());
        levelData.forEach((lvl, data) -> {
            buf.writeVarInt(lvl);
            writeLevelData(buf, data);
        });
    }

    private SkillDefinition decodeDefinition(FriendlyByteBuf buf) {
        SkillDefinition.Builder builder = SkillDefinition.builder(buf.readResourceLocation());
        builder.displayName(buf.readUtf());
        builder.category(buf.readEnum(SkillCategory.class));
        builder.tier(buf.readEnum(SkillTier.class));
        builder.usageType(buf.readEnum(SkillUsageType.class));
        builder.scalingStat(buf.readUtf());
        builder.xpMultiplier(buf.readDouble());

        builder.baseCost(buf.readVarInt());
        builder.costPerLevel(buf.readVarInt());

        builder.cooldownTicks(buf.readVarInt());
        builder.castDelayTicks(buf.readVarInt());
        builder.castTimeTicks(buf.readVarInt());
        builder.interruptible(buf.readBoolean());

        builder.maxLevel(buf.readVarInt());
        builder.upgradeCost(buf.readVarInt());
        builder.canGainXp(buf.readBoolean());
        builder.canUpgradeWithPoints(buf.readBoolean());

        // Requirements
        int reqSize = buf.readVarInt();
        Map<ResourceLocation, Integer> reqs = new HashMap<>();
        for (int i = 0; i < reqSize; i++) {
            reqs.put(buf.readResourceLocation(), buf.readVarInt());
        }
        builder.requirements(reqs);

        // Jobs
        int jobSize = buf.readVarInt();
        Set<String> jobs = new HashSet<>();
        for (int i = 0; i < jobSize; i++) {
            jobs.add(buf.readUtf());
        }
        builder.allowedJobs(jobs);

        // UI
        if (buf.readBoolean()) builder.icon(buf.readResourceLocation());
        builder.textureName(buf.readUtf());
        builder.gridX(buf.readVarInt());
        builder.gridY(buf.readVarInt());

        // Effect
        if (buf.readBoolean()) builder.effectClass(buf.readUtf());

        // Level Data
        int lvlDataSize = buf.readVarInt();
        Map<Integer, SkillLevelData> levelDataMap = new HashMap<>();
        for (int i = 0; i < lvlDataSize; i++) {
            levelDataMap.put(buf.readVarInt(), readLevelData(buf));
        }
        builder.levelData(levelDataMap);

        return builder.build();
    }

    private void writeLevelData(FriendlyByteBuf buf, SkillLevelData data) {
        buf.writeMap(data.getNumericValues(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeDouble);
        buf.writeMap(data.getStringValues(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
        buf.writeMap(data.getBooleanValues(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBoolean);
    }

    private SkillLevelData readLevelData(FriendlyByteBuf buf) {
        Map<String, Double> nums = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readDouble);
        Map<String, String> strs = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
        Map<String, Boolean> bools = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBoolean);
        return new SkillLevelData(nums, strs, bools);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            SkillRegistry.applySync(definitions);
        });
        context.get().setPacketHandled(true);
    }
}
