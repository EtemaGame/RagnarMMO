package com.etema.ragnarmmo.skills.runtime;

import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.skills.api.SkillCategory;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.data.progression.SkillState;
import com.etema.ragnarmmo.skills.api.XPGainReason;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Updated SkillManager that uses ResourceLocation-based skill storage.
 * Manages all skills for a player with proper separation by category.
 *
 * Uses a ResourceLocation-keyed skill map:
 * Map&lt;ResourceLocation, SkillState&gt;
 * for data-driven skill support.
 */
public class SkillManager implements com.etema.ragnarmmo.skills.api.IPlayerSkills {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillManager.class);
    private static final int MAX_WARP_MEMOS = 3;

    public static final class WarpMemo {
        private final ResourceLocation dimensionId;
        private final BlockPos pos;

        public WarpMemo(ResourceLocation dimensionId, BlockPos pos) {
            this.dimensionId = dimensionId;
            this.pos = pos.immutable();
        }

        public ResourceLocation getDimensionId() {
            return dimensionId;
        }

        public BlockPos getPos() {
            return pos;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimensionId.toString());
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            return tag;
        }

        public static WarpMemo fromNBT(CompoundTag tag) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
            if (dimensionId == null) {
                return null;
            }
            return new WarpMemo(dimensionId, new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
        }
    }

    // New data-driven storage
    private final Map<ResourceLocation, SkillState> skills = new HashMap<>();
    private final ItemStackHandler cartInventory;
    private final WarpMemo[] warpMemos = new WarpMemo[MAX_WARP_MEMOS];
    private Player player;
    private int selectedWarpDestination = 0; // 0 = Save Point, 1..3 = Memo slots

    public SkillManager() {
        // Initialize skills from registry
        for (ResourceLocation skillId : SkillRegistry.getAllIds()) {
            skills.put(skillId, new SkillState(skillId));
        }
        this.cartInventory = new ItemStackHandler(54); // Max 6 rows
    }

    public ItemStackHandler getCartInventory() {
        return cartInventory;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public static int getMaxWarpMemos() {
        return MAX_WARP_MEMOS;
    }

    public java.util.Optional<WarpMemo> getWarpMemo(int slot) {
        if (slot < 1 || slot > MAX_WARP_MEMOS) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(warpMemos[slot - 1]);
    }

    public void setWarpMemo(int slot, ResourceLocation dimensionId, BlockPos pos) {
        if (slot < 1 || slot > MAX_WARP_MEMOS || dimensionId == null || pos == null) {
            return;
        }
        warpMemos[slot - 1] = new WarpMemo(dimensionId, pos);
    }

    public void clearWarpMemo(int slot) {
        if (slot < 1 || slot > MAX_WARP_MEMOS) {
            return;
        }
        warpMemos[slot - 1] = null;
    }

    public int getSelectedWarpDestination() {
        return selectedWarpDestination;
    }

    public void setSelectedWarpDestination(int selection) {
        selectedWarpDestination = Math.max(0, Math.min(MAX_WARP_MEMOS, selection));
    }

    // ========================================================================
    // New ResourceLocation-based methods
    // ========================================================================

    /**
     * Gets the state for a specific skill by ResourceLocation.
     *
     * @param skillId The skill ID
     * @return The SkillState, or null if not found
     */
    public SkillState getSkillState(ResourceLocation skillId) {
        return skills.get(skillId);
    }

    /**
     * Gets the level of a specific skill by ResourceLocation.
     */
    @Override
    public int getSkillLevel(ResourceLocation skillId) {
        if (skillId == null)
            return 0;
        SkillState state = skills.get(skillId);
        return state != null ? state.getLevel() : 0;
    }

    /**
     * Gets the XP of a specific skill by ResourceLocation.
     */
    @Override
    public double getSkillXp(ResourceLocation skillId) {
        if (skillId == null)
            return 0;
        SkillState state = skills.get(skillId);
        return state != null ? state.getXp() : 0;
    }

    /**
     * Adds XP to a skill by ResourceLocation.
     *
     * @return number of levels gained
     */
    @Override
    public int addXP(ResourceLocation skillId, double amount, XPGainReason reason) {
        return addXPInternal(skillId, amount, false);
    }

    /**
     * Adds XP through the safe API by ResourceLocation.
     *
     * @return number of levels gained
     */
    @Override
    public int addSkillXP(ResourceLocation skillId, double amount, ChangeReason reason) {
        boolean bypassJobGating = reason != ChangeReason.PLAYER_ACTION;
        return addXPInternal(skillId, amount, bypassJobGating);
    }

    /**
     * Sets skill level by ResourceLocation.
     *
     * @return the final (clamped) level
     */
    @Override
    public int setSkillLevel(ResourceLocation skillId, int level, ChangeReason reason) {
        if (skillId == null)
            return 0;

        SkillState state = skills.get(skillId);
        if (state == null) {
            // Create new state if skill exists in registry but not in our map
            if (SkillRegistry.contains(skillId)) {
                state = new SkillState(skillId);
                skills.put(skillId, state);
            } else {
                return 0;
            }
        }

        state.setLevel(level);

        if (player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(serializeNBT()));
            SkillEffectHandler.refreshPassiveEffects(serverPlayer);
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(serverPlayer).ifPresent(stats -> {
                if (stats instanceof com.etema.ragnarmmo.player.stats.capability.PlayerStats internal) {
                    internal.markDirty(com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.STATS);
                }
            });
        }

        return state.getLevel();
    }

    private int addXPInternal(ResourceLocation skillId, double amount, boolean bypassJobGating) {
        if (skillId == null)
            return 0;

        SkillState state = skills.get(skillId);
        if (state == null)
            return 0;

        // 1. Base amount validation
        if (amount <= 0)
            return 0;

        // 2. Check if skill can gain XP via registry
        Optional<ISkillDefinition> defOpt = SkillRegistry.get(skillId).map(d -> (ISkillDefinition) d);
        if (defOpt.isEmpty())
            return 0;

        ISkillDefinition def = defOpt.get();
        if (!def.canGainXp())
            return 0;

        // 3. Enforce job gating for class-tree skills
        if (!bypassJobGating && player instanceof ServerPlayer serverPlayer
                && def.getCategory() == SkillCategory.CLASS_PASSIVE) {

            // Novice skills are available to everyone
            boolean isNovice = def.getTier() == com.etema.ragnarmmo.skills.api.SkillTier.NOVICE;

            if (!isNovice) {
                var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(serverPlayer);
                if (statsOpt.isEmpty())
                    return 0;
                var stats = statsOpt.get();
                var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(stats.getJobId());

                // Check if job allows this skill
                if (!job.getAllowedSkillIds().contains(skillId)) {
                    return 0;
                }
            }
        }

        int beforeLevel = state.getLevel();

        // 4. Add XP (SkillState handles multipliers and level-ups)
        state.addXP(amount, XPGainReason.SKILL_USE);

        int levelsGained = Math.max(0, state.getLevel() - beforeLevel);

        // 5. Award Base and Job EXP based on skill gain
        if (amount > 0 && player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(serverPlayer).ifPresent(stats -> {
                double baseMult = RagnarConfigs.SERVER.progression.skillToBaseExpMultiplier.get();
                double jobMult = RagnarConfigs.SERVER.progression.skillToJobExpMultiplier.get();
                
                com.etema.ragnarmmo.player.progression.PlayerProgressionService progressionService =
                        com.etema.ragnarmmo.player.progression.PlayerProgressionService
                                .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()));
                int baseExpToGrant = progressionService.applyBaseExpRate((int) Math.round(amount * baseMult));
                int jobExpToGrant = progressionService.applyJobExpRate((int) Math.round(amount * jobMult));

                if (baseExpToGrant > 0) {
                    stats.addExpAndProcessLevelUps(baseExpToGrant, 
                        RagnarConfigs.SERVER.progression.pointsPerLevel.get(), 
                        progressionService::baseExpToNext);
                }
                if (jobExpToGrant > 0) {
                    stats.addJobExpAndProcessLevelUps(jobExpToGrant, 
                        progressionService::jobExpToNext);
                }
                
                // Sync is handled by the caller or by dirty check in tick.
            });

            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.player.stats.network.ClientboundSkillXpPacket(skillId, (int) amount));
        }

        return levelsGained;
    }

    /**
     * Called when a skill levels up.
     */
    protected void onLevelUp(ResourceLocation skillId, int newLevel, int levelsGained) {
        if (player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.player.stats.network.ClientboundLevelUpPacket(skillId, newLevel));
        }
    }

    /**
     * Gets the total combined level of all skills.
     */
    @Override
    public int getTotalLevel() {
        return skills.values().stream()
                .mapToInt(SkillState::getLevel)
                .sum();
    }

    /**
     * Gets the average level across all skills.
     */
    public double getAverageLevel() {
        return skills.values().stream()
                .mapToInt(SkillState::getLevel)
                .average()
                .orElse(1.0);
    }

    /**
     * Gets highest level skill.
     */
    public Optional<SkillState> getHighestSkill() {
        return skills.values().stream()
                .max((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
    }

    /**
     * Attempts to upgrade a skill using a skill point.
     *
     * @param skillId The skill's ResourceLocation
     * @return true if successful
     */
    public boolean tryUpgradeSkill(ResourceLocation skillId) {
        if (player == null || skillId == null)
            return false;

        SkillState state = skills.get(skillId);
        if (state == null) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=missing_state", skillId);
            return false;
        }

        Optional<ISkillDefinition> defOpt = SkillRegistry.get(skillId).map(d -> (ISkillDefinition) d);
        if (defOpt.isEmpty()) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=missing_definition", skillId);
            return false;
        }

        ISkillDefinition def = defOpt.get();

        // Check if skill can be upgraded with skill points
        if (!def.canUpgradeWithPoints()) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=no_point_upgrade", skillId);
            return false;
        }

        // Check max level
        if (state.getLevel() >= def.getMaxLevel()) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=maxed level={}", skillId,
                    state.getLevel());
            return false;
        }

        // 1. Get Stats for Skill Points
        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty()) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=missing_stats", skillId);
            return false;
        }
        var stats = statsOpt.get();

        if (stats.getSkillPoints() <= 0) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=no_points", skillId);
            return false;
        }

        // 2. Check Job Allow
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(stats.getJobId());
        Set<String> allowedJobs = def.getAllowedJobs();
        
        // Exact job, inherited first-job access, and Novice carryover are resolved
        // centrally by JobType.
        if (!allowedJobs.isEmpty()) {
            boolean jobAllowed = allowedJobs.stream().anyMatch(job::matchesSkillRule);

            if (!jobAllowed) {
                RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=job job={}", skillId,
                        job.getId());
                return false;
            }
        }

        // 3. Check Dependencies
        Map<ResourceLocation, Integer> reqs = def.getRequirements();
        for (var entry : reqs.entrySet()) {
            if (getSkillLevel(entry.getKey()) < entry.getValue()) {
                RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=requirement required={} level={}",
                        skillId, entry.getKey(), entry.getValue());
                return false;
            }
        }

        // 4. Get upgrade cost
        int upgradeCost = def.getUpgradeCost();
        if (stats.getSkillPoints() < upgradeCost) {
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=cost needed={} points={}", skillId,
                    upgradeCost, stats.getSkillPoints());
            return false;
        }

        // 5. Upgrade
        boolean upgraded = state.upgradeLevel(def.getMaxLevel());

        if (upgraded) {
            stats.setSkillPoints(stats.getSkillPoints() - upgradeCost);
            onLevelUp(skillId, state.getLevel(), 1);
            RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=ok newLevel={} remainingPoints={}", skillId,
                    state.getLevel(), stats.getSkillPoints());

            // Sync stats (skill points) and skills to client
            if (player instanceof ServerPlayer sp) {
                SkillEffectHandler.refreshPassiveEffects(sp);
                com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService.sync(sp, stats,
                        com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.PROGRESSION.bit());
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(sp,
                        new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(this.serializeNBT()));
            }
            return true;
        }

        RagnarDebugLog.playerData("SKILL_UPGRADE skill={} result=reject reason=upgrade_failed", skillId);
        return false;
    }

    // === Hotbar Management ===
    private final String[] hotbar = new String[9];

    // === Casting System ===
    private ResourceLocation activeCastSkillId;
    private int activeCastLevel;
    private int castTicksRemaining;
    private int castTotalTicks;

    public void startCast(ResourceLocation skillId, int level, int duration) {
        this.activeCastSkillId = skillId;
        this.activeCastLevel = Math.max(1, level);
        this.castTotalTicks = duration;
        this.castTicksRemaining = duration;

        if (player instanceof ServerPlayer serverPlayer) {
            String skillIdStr = skillId != null ? skillId.toString() : "";
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.skills.net.ClientboundCastUpdatePacket(skillIdStr, duration,
                            duration));
        }
    }

    public void interruptCast() {
        if (activeCastSkillId != null) {
            this.activeCastSkillId = null;
            this.activeCastLevel = 0;
            this.castTicksRemaining = 0;
            this.castTotalTicks = 0;

            if (player instanceof ServerPlayer serverPlayer) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new com.etema.ragnarmmo.skills.net.ClientboundCastUpdatePacket("", 0, 0));
            }
        }
    }

    public boolean isCasting() {
        return activeCastSkillId != null;
    }

    public ResourceLocation getActiveCastSkillId() {
        return activeCastSkillId;
    }

    public int getActiveCastLevel() {
        return activeCastLevel;
    }

    public int getCastTicksRemaining() {
        return castTicksRemaining;
    }

    public int getCastTotalTicks() {
        return castTotalTicks;
    }

    /**
     * Ticks the casting process.
     *
     * @return true if the cast just finished.
     */
    public boolean tickCast() {
        applyPendingCooldowns();

        if (activeCastSkillId != null) {
            castTicksRemaining--;
            if (castTicksRemaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public void clearCast() {
        this.interruptCast();
    }

    public String[] getHotbar() {
        return hotbar;
    }

    public void setHotbarSlot(int slot, String skillId) {
        if (slot < 0 || slot >= 9) {
            return;
        }

        String normalizedSkillId = skillId == null ? "" : skillId.trim();
        if (normalizedSkillId.isEmpty()) {
            hotbar[slot] = null;
        } else {
            ResourceLocation parsedId = parseMirrorSkillId(normalizedSkillId);
            if (parsedId == null) {
                return;
            }

            Optional<ISkillDefinition> definition = SkillRegistry.get(parsedId).map(d -> (ISkillDefinition) d);
            SkillState state = skills.get(parsedId);
            if (definition.isEmpty() || state == null || state.getLevel() <= 0 || !definition.get().isActive()) {
                return;
            }

            hotbar[slot] = parsedId.toString();
        }

        if (player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(serializeNBT()));
        }
    }

    /**
     * Serializes all skills to NBT.
     * Uses full ResourceLocation format (e.g., "ragnarmmo:bash") for future
     * compatibility.
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        applyPendingCooldowns();

        // Serialize skills with full ResourceLocation
        for (Map.Entry<ResourceLocation, SkillState> entry : skills.entrySet()) {
            CompoundTag skillTag = new CompoundTag();
            skillTag.putInt("level", entry.getValue().getLevel());
            skillTag.putDouble("xp", entry.getValue().getXp());
            skillTag.putLong("lastProcKv", entry.getValue().getLastProcTime());
            tag.put(entry.getKey().toString(), skillTag);
        }

        tag.put("cartInventory", cartInventory.serializeNBT());

        // Hotbar serialization
        net.minecraft.nbt.ListTag hotbarTag = new net.minecraft.nbt.ListTag();
        for (String s : hotbar) {
            hotbarTag.add(net.minecraft.nbt.StringTag.valueOf(s == null ? "" : s));
        }
        tag.put("hotbar", hotbarTag);

        // Cooldown serialization
        if (player != null) {
            CompoundTag cooldownTag = new CompoundTag();
            long now = player.level().getGameTime();
            for (Map.Entry<ResourceLocation, Long> entry : cooldowns.entrySet()) {
                long remaining = entry.getValue() - now;
                if (remaining > 0) {
                    cooldownTag.putLong(entry.getKey().toString(), remaining);
                }
            }
            tag.put("cooldowns", cooldownTag);

            long globalRemaining = globalCooldownUntil - now;
            if (globalRemaining > 0) {
                tag.putLong("globalCooldown", globalRemaining);
                tag.putInt("globalCooldownDuration", globalCooldownDuration);
            }
        }

        CompoundTag warpTag = new CompoundTag();
        warpTag.putInt("selectedDestination", selectedWarpDestination);
        for (int i = 0; i < MAX_WARP_MEMOS; i++) {
            WarpMemo memo = warpMemos[i];
            if (memo != null) {
                warpTag.put("memo_" + (i + 1), memo.serializeNBT());
            }
        }
        tag.put("warpPortal", warpTag);

        return tag;
    }

    /**
     * Deserializes all skills from NBT using canonical skill ids only.
     */
    public void deserializeNBT(CompoundTag tag) {
        // First, reset all skills to ensure clean state
        for (SkillState state : skills.values()) {
            state.reset();
        }

        Arrays.fill(hotbar, null);
        for (int i = 0; i < cartInventory.getSlots(); i++) {
            cartInventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        activeCastSkillId = null;
        activeCastLevel = 0;
        castTicksRemaining = 0;
        castTotalTicks = 0;
        cooldowns.clear();
        cooldownDurations.clear();
        pendingCooldowns.clear();
        globalCooldownUntil = 0L;
        globalCooldownDuration = 0;

        // Load skills from NBT
        for (String key : tag.getAllKeys()) {
            // Skip non-skill keys
            if (key.equals("cartInventory") || key.equals("hotbar") || key.equals("cooldowns")
                    || key.equals("globalCooldown") || key.equals("globalCooldownDuration")
                    || key.equals("warpPortal")) {
                continue;
            }

            ResourceLocation skillId = ResourceLocation.tryParse(key);
            if (skillId == null) {
                LOGGER.warn("Failed to parse skill ID from NBT key: {}", key);
                continue;
            }

            // Get or create skill state
            SkillState state = skills.get(skillId);
            if (state == null) {
                // Skill might have been added after world save, check registry
                if (SkillRegistry.contains(skillId)) {
                    state = new SkillState(skillId);
                    skills.put(skillId, state);
                } else {
                    LOGGER.warn("Unknown skill in NBT: {} (key: {})", skillId, key);
                    continue;
                }
            }

            // Load skill data
            CompoundTag skillTag = tag.getCompound(key);
            state.setLevel(skillTag.getInt("level"));
            state.setXp(skillTag.getDouble("xp"));
            if (skillTag.contains("lastProcKv")) {
                state.setLastProcTime(skillTag.getLong("lastProcKv"));
            }
        }

        // Load cart inventory
        if (tag.contains("cartInventory")) {
            cartInventory.deserializeNBT(tag.getCompound("cartInventory"));
        }

        // Load hotbar
        if (tag.contains("hotbar")) {
            net.minecraft.nbt.ListTag hotbarTag = tag.getList("hotbar", 8); // 8 = StringTag
            for (int i = 0; i < 9 && i < hotbarTag.size(); i++) {
                hotbar[i] = hotbarTag.getString(i);
                if (hotbar[i].isEmpty())
                    hotbar[i] = null;
            }
        }

        if (tag.contains("cooldowns")) {
            CompoundTag cooldownTag = tag.getCompound("cooldowns");
            for (String key : cooldownTag.getAllKeys()) {
                ResourceLocation skillId = ResourceLocation.tryParse(key);
                if (skillId != null) {
                    long remaining = cooldownTag.getLong(key);
                    if (remaining > 0) {
                        pendingCooldowns.put(skillId, remaining);
                        int duration = SkillRegistry.get(skillId)
                                .map(ISkillDefinition::getCooldownTicks)
                                .orElse((int) Math.min(Integer.MAX_VALUE, remaining));
                        cooldownDurations.put(skillId, Math.max(1, duration));
                    }
                }
            }
        }

        if (tag.contains("globalCooldown") && player != null) {
            long remaining = tag.getLong("globalCooldown");
            if (remaining > 0) {
                globalCooldownUntil = player.level().getGameTime() + remaining;
                globalCooldownDuration = tag.contains("globalCooldownDuration")
                        ? Math.max(1, tag.getInt("globalCooldownDuration"))
                        : (int) Math.min(Integer.MAX_VALUE, remaining);
            }
        }

        selectedWarpDestination = 0;
        Arrays.fill(warpMemos, null);
        if (tag.contains("warpPortal")) {
            CompoundTag warpTag = tag.getCompound("warpPortal");
            selectedWarpDestination = Math.max(0,
                    Math.min(MAX_WARP_MEMOS, warpTag.getInt("selectedDestination")));
            for (int i = 0; i < MAX_WARP_MEMOS; i++) {
                String key = "memo_" + (i + 1);
                if (warpTag.contains(key)) {
                    warpMemos[i] = WarpMemo.fromNBT(warpTag.getCompound(key));
                }
            }
        }

        applyPendingCooldowns();
    }

    /**
     * Resets all skills to level 0.
     */
    public void resetAll() {
        resetAll(ChangeReason.SYSTEM);
    }

    @Override
    public void resetAll(ChangeReason reason) {
        for (SkillState state : skills.values()) {
            state.reset();
        }

        Arrays.fill(hotbar, null);
        activeCastSkillId = null;
        activeCastLevel = 0;
        castTicksRemaining = 0;
        castTotalTicks = 0;
        selectedWarpDestination = 0;
        cooldowns.clear();
        cooldownDurations.clear();
        pendingCooldowns.clear();
        globalCooldownUntil = 0L;
        globalCooldownDuration = 0;
        Arrays.fill(warpMemos, null);

        for (int i = 0; i < cartInventory.getSlots(); i++) {
            cartInventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(serializeNBT()));
            SkillEffectHandler.refreshPassiveEffects(serverPlayer);
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(serverPlayer).ifPresent(stats -> {
                if (stats instanceof com.etema.ragnarmmo.player.stats.capability.PlayerStats internal) {
                    internal.markDirty(com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.STATS);
                }
            });
        }
    }

    // === Cooldown Management (Transient) ===
    private final Map<ResourceLocation, Long> cooldowns = new HashMap<>();
    private final Map<ResourceLocation, Integer> cooldownDurations = new HashMap<>();
    private final Map<ResourceLocation, Long> pendingCooldowns = new HashMap<>();

    // Global Cooldown (Cast Delay)
    private long globalCooldownUntil = 0L;
    private int globalCooldownDuration = 0;

    public boolean isOnGlobalCooldown() {
        return player != null && player.level().getGameTime() < globalCooldownUntil;
    }

    public void setGlobalCooldown(int ticks) {
        if (player != null && ticks > 0) {
            globalCooldownUntil = player.level().getGameTime() + ticks;
            globalCooldownDuration = ticks;
        }
    }

    public boolean isOnCooldown(ResourceLocation skillId) {
        applyPendingCooldowns();
        return player != null && player.level().getGameTime() < cooldowns.getOrDefault(skillId, 0L);
    }

    public void setCooldown(ResourceLocation skillId, int ticks) {
        if (player != null && skillId != null) {
            cooldowns.put(skillId, player.level().getGameTime() + ticks);
            cooldownDurations.put(skillId, ticks);
        }
    }

    public long getCooldownTicksRemaining(ResourceLocation skillId) {
        applyPendingCooldowns();
        if (player == null || skillId == null)
            return 0L;
        long now = player.level().getGameTime();
        long localEnd = cooldowns.getOrDefault(skillId, 0L);
        long globalEnd = globalCooldownUntil;
        return Math.max(0, Math.max(localEnd - now, globalEnd - now));
    }

    public float getCooldownProgress(ResourceLocation skillId, float partialTick) {
        applyPendingCooldowns();
        if (player == null || skillId == null)
            return 0f;

        long now = player.level().getGameTime();

        // Check local cooldown
        long localEnd = cooldowns.getOrDefault(skillId, 0L);
        int localDuration = cooldownDurations.getOrDefault(skillId, 0);
        
        // If the duration is not mirrored yet (for example right after sync),
        // recover it from the registry for local UI pacing.
        if (localDuration <= 0 && now < localEnd) {
            localDuration = SkillRegistry.get(skillId)
                .map(com.etema.ragnarmmo.skills.api.ISkillDefinition::getCooldownTicks)
                .orElse(1);
            // Cache it for this session
            cooldownDurations.put(skillId, localDuration);
        }

        float localProgress = 0f;
        if (now < localEnd && localDuration > 0) {
            localProgress = (float) (localEnd - now) / (float) localDuration;
        }

        // Check global cooldown
        float globalProgress = 0f;
        if (now < globalCooldownUntil && globalCooldownDuration > 0) {
            globalProgress = (float) (globalCooldownUntil - now) / (float) globalCooldownDuration;
        }

        // Return the most restrictive (highest progress bar value)
        return Math.max(0, Math.min(1.0f, Math.max(localProgress, globalProgress)));
    }

    private void applyPendingCooldowns() {
        if (player != null && !pendingCooldowns.isEmpty()) {
            long now = player.level().getGameTime();
            for (Map.Entry<ResourceLocation, Long> entry : pendingCooldowns.entrySet()) {
                cooldowns.put(entry.getKey(), now + entry.getValue());
            }
            pendingCooldowns.clear();
        }
    }

    /**
     * Ensures a skill exists in this manager's state map.
     * Called when registry adds new skills after initial construction.
     *
     * @param skillId The skill ID to ensure
     */
    public void ensureSkillExists(ResourceLocation skillId) {
        if (!skills.containsKey(skillId) && SkillRegistry.contains(skillId)) {
            skills.put(skillId, new SkillState(skillId));
        }
    }

    /**
     * Applies a server snapshot to the client-side skill manager.
     *
     * @param nbt The authoritative server snapshot.
     */
    public void applyClientMirror(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty())
            return;

        CompoundTag skillsTag = nbt;

        // Reset learned state first so server-side removals/unlocks are mirrored exactly.
        for (SkillState state : skills.values()) {
            state.reset();
        }

        for (String key : skillsTag.getAllKeys()) {
            if (isMirrorReservedKey(key)) {
                continue;
            }

            ResourceLocation id = parseMirrorSkillId(key);
            if (id == null) {
                continue;
            }

            CompoundTag stateTag = skillsTag.getCompound(key);

            SkillState state = skills.computeIfAbsent(id, SkillState::new);
            state.setLevel(stateTag.getInt("level"));
            state.setXp(stateTag.getDouble("xp"));
            if (stateTag.contains("lastProcKv")) {
                state.setLastProcTime(stateTag.getLong("lastProcKv"));
            }
        }

        Arrays.fill(hotbar, null);
        if (nbt.contains("hotbar")) {
            net.minecraft.nbt.ListTag hotbarTag = nbt.getList("hotbar", 8);
            for (int i = 0; i < 9 && i < hotbarTag.size(); i++) {
                hotbar[i] = hotbarTag.getString(i);
                if (hotbar[i].isEmpty()) {
                    hotbar[i] = null;
                }
            }
        }

        for (int i = 0; i < cartInventory.getSlots(); i++) {
            cartInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (nbt.contains("cartInventory")) {
            cartInventory.deserializeNBT(nbt.getCompound("cartInventory"));
        }

        cooldowns.clear();
        cooldownDurations.clear();
        pendingCooldowns.clear();
        globalCooldownUntil = 0L;
        globalCooldownDuration = 0;

        if (nbt.contains("cooldowns")) {
            CompoundTag cooldownTag = nbt.getCompound("cooldowns");
            for (String key : cooldownTag.getAllKeys()) {
                ResourceLocation skillId = parseMirrorSkillId(key);
                if (skillId == null) {
                    continue;
                }

                long remaining = cooldownTag.getLong(key);
                if (remaining > 0) {
                    pendingCooldowns.put(skillId, remaining);
                    int duration = SkillRegistry.get(skillId)
                            .map(ISkillDefinition::getCooldownTicks)
                            .orElse((int) Math.min(Integer.MAX_VALUE, remaining));
                    cooldownDurations.put(skillId, Math.max(1, duration));
                }
            }
        }

        if (nbt.contains("globalCooldown") && player != null) {
            long remaining = nbt.getLong("globalCooldown");
            if (remaining > 0) {
                globalCooldownUntil = player.level().getGameTime() + remaining;
                globalCooldownDuration = nbt.contains("globalCooldownDuration")
                        ? Math.max(1, nbt.getInt("globalCooldownDuration"))
                        : (int) Math.min(Integer.MAX_VALUE, remaining);
            }
        }

        selectedWarpDestination = 0;
        Arrays.fill(warpMemos, null);
        if (nbt.contains("warpPortal")) {
            CompoundTag warpTag = nbt.getCompound("warpPortal");
            selectedWarpDestination = Math.max(0,
                    Math.min(MAX_WARP_MEMOS, warpTag.getInt("selectedDestination")));
            for (int i = 0; i < MAX_WARP_MEMOS; i++) {
                String memoKey = "memo_" + (i + 1);
                if (warpTag.contains(memoKey)) {
                    warpMemos[i] = WarpMemo.fromNBT(warpTag.getCompound(memoKey));
                }
            }
        }

        applyPendingCooldowns();
    }

    private static boolean isMirrorReservedKey(String key) {
        return "Skills".equals(key)
                || "cartInventory".equals(key)
                || "hotbar".equals(key)
                || "cooldowns".equals(key)
                || "globalCooldown".equals(key)
                || "globalCooldownDuration".equals(key)
                || "warpPortal".equals(key);
    }

    private static ResourceLocation parseMirrorSkillId(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(key);
    }
}
