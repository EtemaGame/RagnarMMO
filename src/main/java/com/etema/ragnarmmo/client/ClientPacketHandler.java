package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.client.effects.EffectTriggerPhase;
import com.etema.ragnarmmo.client.effects.EffectVec3;
import com.etema.ragnarmmo.client.effects.runtime.EffectContext;
import com.etema.ragnarmmo.client.effects.runtime.SkillEffectSpawner;
import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.lifeskills.LifeSkillCapability;
import com.etema.ragnarmmo.lifeskills.LifeSkillClientHandler;
import com.etema.ragnarmmo.lifeskills.LifeSkillProgress;

import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncPacket;
import com.etema.ragnarmmo.player.party.PartyClientData;
import com.etema.ragnarmmo.player.party.net.PartyMemberData;
import com.etema.ragnarmmo.client.render.RagnarPopoffHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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

    public static void openCardCompoundScreen(int slotIndex, net.minecraft.world.item.ItemStack stack) {
        Minecraft.getInstance().setScreen(new com.etema.ragnarmmo.client.ui.CardCompoundScreen(slotIndex, stack));
    }

    public static void handleAchievementsSync(int entityId, CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(entityId);
        
        // Use the local player directly if the level entity map has not caught up yet.
        if (entity == null && mc.player != null && mc.player.getId() == entityId) {
            entity = mc.player;
        }

        if (entity != null) {
            entity.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
                cap.deserializeNBT(tag);
            });
        } else {
            com.etema.ragnarmmo.RagnarMMO.LOGGER.warn("Received achievement sync for unknown entity: {}", entityId);
        }
    }

    // ═══════════════════════════════════════════════
    // PlayerStatsSyncPacket
    // ═══════════════════════════════════════════════
    public static void handlePlayerStatsSync(PlayerStatsSyncPacket msg) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        
        net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.entityId);
        if (entity == null && mc.player != null && mc.player.getId() == msg.entityId) {
            entity = mc.player; // Direct local mirror during very early join
        }

        if (!(entity instanceof net.minecraft.world.entity.player.Player p)) {
            return;
        }

        RagnarCoreAPI.get(p).ifPresent(s -> {
            if (s instanceof com.etema.ragnarmmo.player.stats.capability.PlayerStats ps) {
                ps.applyMirrorState(msg);
            } else {
                // Generic mirror path for non-concrete implementations
                if (RoPlayerSyncDomain.includes(msg.syncMask, RoPlayerSyncDomain.RESOURCES)) {
                    s.setMana(msg.mana);
                    s.setManaMaxClient(msg.manaMax);
                }

                if (RoPlayerSyncDomain.includes(msg.syncMask, RoPlayerSyncDomain.PROGRESSION)) {
                    s.setJobId(msg.jobId);
                    s.setLevel(msg.level);
                    s.setExp(msg.exp);
                    s.setStatPoints(msg.statPoints);
                    s.setJobLevel(msg.jobLevel);
                    s.setJobExp(msg.jobExp);
                    s.setSkillPoints(msg.skillPoints);
                }

                if (RoPlayerSyncDomain.includes(msg.syncMask, RoPlayerSyncDomain.STATS)) {
                    s.setSTR(msg.str);
                    s.setAGI(msg.agi);
                    s.setVIT(msg.vit);
                    s.setINT(msg.intelligence);
                    s.setDEX(msg.dex);
                    s.setLUK(msg.luk);
                }
            }
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

    public static void handleSkillXpGain(net.minecraft.resources.ResourceLocation skillId, int amount) {
        if (skillId == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        // Client no longer mutates skill state directly to ensure authority
        // Skill progress will be updated via Sync packets from server
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
            manager.applyClientMirror(data);
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
                id = ResourceLocation.fromNamespaceAndPath("ragnarmmo", skillId.toLowerCase());
            }
        }
        ClientCastManager.getInstance().updateCast(id, currentTicks, totalTicks);
    }

    // ═══════════════════════════════════════════════
    // Mob profile sync
    // ═══════════════════════════════════════════════
    public static void handleMobProfileSync(int entityId, MobProfile profile) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(entityId);
        if (!(e instanceof LivingEntity living)) {
            return;
        }
        MobProfileProvider.get(living).ifPresent(state -> state.setProfile(profile));
    }

    public static void handleMobProfileClear(int entityId) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(entityId);
        if (!(e instanceof LivingEntity living)) {
            return;
        }
        MobProfileProvider.get(living).ifPresent(state -> state.clearProfile());
    }

    // ═══════════════════════════════════════════════
    // Wallet Sync Packet

    // ═══════════════════════════════════════════════
    public static void handleWalletSync(com.etema.ragnarmmo.economy.zeny.network.WalletSyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            com.etema.ragnarmmo.economy.zeny.capability.PlayerWalletProvider.get(mc.player).ifPresent(wallet -> {
                wallet.setZeny(msg.zeny);
            });
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
    public static void handleDerivedStatsSync(com.etema.ragnarmmo.player.stats.network.DerivedStatsSyncPacket msg) {
        DerivedStatsClientCache.update(msg.toDerivedStats());
    }

    // SyncRoItemRulesPacket
    // ═══════════════════════════════════════════════
    public static void handleRoItemRulesSync(
            java.util.Map<net.minecraft.resources.ResourceLocation, com.etema.ragnarmmo.items.data.RoItemRule> itemRules,
            java.util.Map<net.minecraft.resources.ResourceLocation, com.etema.ragnarmmo.items.data.RoItemRule> tagRules,
            java.util.Map<String, java.util.Map<com.etema.ragnarmmo.items.cards.CardEquipType, com.etema.ragnarmmo.items.data.RoItemRule>> modTypeRules) {
        com.etema.ragnarmmo.items.data.RoItemRuleLoader.applyClientSync(itemRules, tagRules, modTypeRules);
    }

    // ═══════════════════════════════════════════════
    // SkillPhaseWorldEffectPacket
    // ═══════════════════════════════════════════════
    public static void handleSkillPhaseWorldEffect(ResourceLocation skillId, EffectTriggerPhase phase, Vec3 position,
            Vec3 normal, float scaleMultiplier, int durationOverrideTicks) {
        EffectContext.Builder context = EffectContext.builder()
                .normal(new EffectVec3((float) normal.x, (float) normal.y, (float) normal.z))
                .scaleMultiplier(scaleMultiplier);

        if (durationOverrideTicks > 0) {
            context.durationOverrideTicks(durationOverrideTicks);
        }

        SkillEffectSpawner.spawnWorldPhaseEffects(position, skillId, phase, context.build());
    }
    // ═══════════════════════════════════════════════
    // ClientboundRagnarCombatResultPacket
    // ═══════════════════════════════════════════════
    public static void handleCombatFeedback(int targetId, com.etema.ragnarmmo.combat.api.CombatHitResultType result, double amount, boolean critical) {
        String text;
        int color;

        switch (result) {
            case HIT -> {
                text = formatDamageAmount(amount);
                color = 0xFFE8EDF5;
            }
            case CRIT -> {
                text = formatDamageAmount(amount);
                color = 0xFFFF5C5C;
            }
            case MISS -> {
                text = "Miss";
                color = 0xFF9BA7B4;
            }
            case DODGE -> {
                text = "Dodge";
                color = 0xFF9BA7B4;
            }
            default -> {
                return;
            }
        }

        RagnarPopoffHandler.addPopoff(targetId, text, color);
    }

    private static String formatDamageAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return "0";
        }
        return String.valueOf(Math.max(1, Math.round(amount)));
    }
}
