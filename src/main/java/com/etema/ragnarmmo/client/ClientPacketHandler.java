package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillCapability;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillClientHandler;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillProgress;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import com.etema.ragnarmmo.system.skills.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket;
import com.etema.ragnarmmo.system.stats.party.PartyClientData;
import com.etema.ragnarmmo.system.stats.party.net.PartyMemberData;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/**
 * Centralized client-side packet handler.
 * This class is ONLY loaded on the client (Dist.CLIENT).
 * All methods here are safe to reference client-only classes like Minecraft,
 * LocalPlayer, etc.
 *
 * Packets delegate to this class via DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
 * () -> () -> ...)
 * so that the server JVM never loads this class.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    // ═══════════════════════════════════════════════
    // PlayerStatsSyncPacket
    // ═══════════════════════════════════════════════
    public static void handlePlayerStatsSync(PlayerStatsSyncPacket msg) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null)
            return;

        RagnarCoreAPI.get(p).ifPresent(s -> {
            s.setMana(msg.mana);
            s.setManaMaxClient(msg.manaMax);
            if (s instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps) {
                ps.setSP(msg.sp);
                ps.setSPMaxClient(msg.spMax);
            }
            s.setLevel(msg.level);
            s.setExp(msg.exp);
            s.setStatPoints(msg.statPoints);
            s.setJobLevel(msg.jobLevel);
            s.setJobExp(msg.jobExp);
            s.setSkillPoints(msg.skillPoints);
            s.setJobId(msg.jobId);
            s.setSTR(msg.str);
            s.setAGI(msg.agi);
            s.setVIT(msg.vit);
            s.setINT(msg.intelligence);
            s.setDEX(msg.dex);
            s.setLUK(msg.luk);
        });
    }

    // ═══════════════════════════════════════════════
    // ClientboundLevelUpPacket
    // ═══════════════════════════════════════════════
    public static void handleSkillLevelUp(net.minecraft.resources.ResourceLocation skillId, int newLevel) {
        if (skillId == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerSkillsProvider.get(mc.player).ifPresent(manager -> {
                manager.setSkillLevel(skillId, newLevel, com.etema.ragnarmmo.common.api.stats.ChangeReason.DEBUG);
            });
        }

        SkillOverlay.showLevelUp(skillId, newLevel);
    }

    // ═══════════════════════════════════════════════
    // ClientboundSkillXpPacket
    // ═══════════════════════════════════════════════
    public static void handleSkillXpGain(net.minecraft.resources.ResourceLocation skillId, int amount) {
        if (skillId == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerSkillsProvider.get(mc.player).ifPresent(manager -> {
                int levels = manager.addSkillXP(skillId, amount,
                        com.etema.ragnarmmo.common.api.stats.ChangeReason.DEBUG);
                if (levels > 0) {
                    SkillOverlay.showLevelUp(skillId, manager.getSkillLevel(skillId));
                }
            });
        }

        SkillOverlay.showXpGain(skillId, amount);
    }

    // ═══════════════════════════════════════════════
    // ClientboundSkillSyncPacket
    // ═══════════════════════════════════════════════
    public static void handleSkillSync(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        PlayerSkillsProvider.get(mc.player).ifPresent(manager -> {
            manager.deserializeNBT(data);
        });
    }

    // ═══════════════════════════════════════════════
    // ClientboundCastUpdatePacket
    // ═══════════════════════════════════════════════
    public static void handleCastUpdate(String skillId, int currentTicks, int totalTicks) {
        net.minecraft.resources.ResourceLocation id = null;
        if (skillId != null && !skillId.isEmpty()) {
            if (skillId.contains(":")) {
                id = net.minecraft.resources.ResourceLocation.tryParse(skillId);
            } else {
                id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ragnarmmo", skillId.toLowerCase());
            }
        }
        ClientCastManager.getInstance().updateCast(id, currentTicks, totalTicks);
    }

    // ═══════════════════════════════════════════════
    // SyncMobStatsPacket
    // ═══════════════════════════════════════════════
    public static void handleMobStatsSync(int entityId, int level, MobTier tier,
            String mobClassName,
            double hpMult, double dmgMult,
            double defMult, double spdMult) {
        var mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        Entity e = mc.level.getEntity(entityId);
        if (!(e instanceof LivingEntity living))
            return;

        MobClass mobClass = safeMobClass(mobClassName);

        MobStatsProvider.get(living).ifPresent(stats -> {
            stats.setLevel(level);
            stats.setTier(tier);
            stats.setMobClass(mobClass);
            stats.setHealthMultiplier(hpMult);
            stats.setDamageMultiplier(dmgMult);
            stats.setDefenseMultiplier(defMult);
            stats.setSpeedMultiplier(spdMult);
            stats.setInitialized(true);
        });
    }

    private static MobClass safeMobClass(String name) {
        if (name == null || name.isEmpty())
            return null;
        try {
            return MobClass.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return MobClass.WARRIOR;
        }
    }

    // ═══════════════════════════════════════════════
    // LifeSkillSyncPacket
    // ═══════════════════════════════════════════════
    public static void handleLifeSkillSync(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            LifeSkillCapability.get(mc.player).ifPresent(manager -> {
                if (data != null) {
                    manager.deserializeNBT(data);
                }
            });
        }
    }

    // ═══════════════════════════════════════════════
    // LifeSkillUpdatePacket
    // ═══════════════════════════════════════════════
    public static void handleLifeSkillUpdate(LifeSkillType skillType, int level, int points) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && skillType != null) {
            LifeSkillCapability.get(mc.player).ifPresent(manager -> {
                LifeSkillProgress progress = manager.getSkill(skillType);
                if (progress != null) {
                    progress.setLevelAndPoints(level, points);
                }
            });
        }
    }

    // ═══════════════════════════════════════════════
    // LifeSkillPointsPacket
    // ═══════════════════════════════════════════════
    public static void handleLifeSkillPointsGain(LifeSkillType skillType, int pointsGained,
            int currentLevel, int currentPoints) {
        if (skillType != null) {
            LifeSkillClientHandler.showPointsGain(skillType, pointsGained, currentLevel, currentPoints);
        }
    }

    // ═══════════════════════════════════════════════
    // LifeSkillLevelUpPacket
    // ═══════════════════════════════════════════════
    public static void handleLifeSkillLevelUp(LifeSkillType skillType, int newLevel) {
        if (skillType != null) {
            LifeSkillClientHandler.showLevelUp(skillType, newLevel);
        }
    }

    // ═══════════════════════════════════════════════
    // LifeSkillPerkChoicePacket
    // ═══════════════════════════════════════════════
    public static void handleLifeSkillPerkChoice(LifeSkillType skillType, int tier) {
        if (skillType != null) {
            LifeSkillClientHandler.showPerkChoice(skillType, tier);
        }
    }

    // ═══════════════════════════════════════════════
    // PartySnapshotS2CPacket
    // ═══════════════════════════════════════════════
    public static void handlePartySnapshot(boolean hasParty, UUID partyId, String partyName,
            List<PartyMemberData> members) {
        if (hasParty) {
            PartyClientData.setParty(partyId, partyName, members);
        } else {
            PartyClientData.clearParty();
        }
    }

    // ═══════════════════════════════════════════════
    // PartyMemberUpdateS2CPacket
    // ═══════════════════════════════════════════════
    public static void handlePartyMemberUpdate(PartyMemberData memberData) {
        PartyClientData.updateMember(memberData);
    }

    // ═══════════════════════════════════════════════
    // DerivedStatsSyncPacket
    // ═══════════════════════════════════════════════
    public static void handleDerivedStatsSync(com.etema.ragnarmmo.system.stats.net.DerivedStatsSyncPacket msg) {
        DerivedStatsClientCache.update(msg.toDerivedStats());
    }

    // ═══════════════════════════════════════════════
    // MobHurtPacket
    // ═══════════════════════════════════════════════
    public static void handleMobHurt(int entityId) {
        var mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        var entity = mc.level.getEntity(entityId);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            com.etema.ragnarmmo.client.render.RagnarBarRenderHandler.markEntityHurt(living);
        }
    }

    // ═══════════════════════════════════════════════
    // SyncRoItemRulesPacket
    // ═══════════════════════════════════════════════
    public static void handleRoItemRulesSync(
            java.util.Map<net.minecraft.resources.ResourceLocation, com.etema.ragnarmmo.roitems.data.RoItemRule> itemRules,
            java.util.Map<net.minecraft.resources.ResourceLocation, com.etema.ragnarmmo.roitems.data.RoItemRule> tagRules) {
        com.etema.ragnarmmo.roitems.data.RoItemRuleLoader.applyClientSync(itemRules, tagRules);
    }
}
