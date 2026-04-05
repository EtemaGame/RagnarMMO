package com.etema.ragnarmmo.skill.runtime;

import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.api.SkillCategory;
import com.etema.ragnarmmo.skill.api.SkillType;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.data.progression.SkillState;
import com.etema.ragnarmmo.skill.api.XPGainReason;
import com.etema.ragnarmmo.skill.data.progression.SkillProgress;
import com.etema.ragnarmmo.skill.data.progression.SkillProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.Locale;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Updated SkillManager that uses ResourceLocation-based skill storage.
 * Manages all skills for a player with proper separation by category.
 *
 * Migrated from EnumMap&lt;SkillType, SkillProgress&gt; to
 * Map&lt;ResourceLocation, SkillState&gt;
 * for data-driven skill support.
 */
public class SkillManager implements com.etema.ragnarmmo.skill.api.IPlayerSkills {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillManager.class);
    private static final String DEFAULT_NAMESPACE = "ragnarmmo";

    // New data-driven storage
    private final Map<ResourceLocation, SkillState> skills = new HashMap<>();
    private final ItemStackHandler cartInventory;
    private Player player;

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
                    new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(serializeNBT()));
            SkillEffectHandler.refreshPassiveEffects(serverPlayer);
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
            boolean isNovice = def.getTier() == com.etema.ragnarmmo.skill.api.SkillTier.NOVICE;

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
                
                int baseExpToGrant = (int) Math.round(amount * baseMult);
                int jobExpToGrant = (int) Math.round(amount * jobMult);

                if (baseExpToGrant > 0) {
                    stats.addExpAndProcessLevelUps(baseExpToGrant, 
                        RagnarConfigs.SERVER.progression.pointsPerLevel.get(), 
                        com.etema.ragnarmmo.system.stats.progression.ExpTable::expToNext);
                }
                if (jobExpToGrant > 0) {
                    stats.addJobExpAndProcessLevelUps(jobExpToGrant, 
                        com.etema.ragnarmmo.system.stats.progression.ExpTable::jobExpToNext);
                }
                
                if (baseExpToGrant > 0 || jobExpToGrant > 0) {
                    stats.markDirty();
                    // Sync is handled by the caller or by dirty check in tick
                }
            });

            // Use legacy packet with SkillType if available for compatibility
            SkillType legacyType = SkillType.fromResourceLocation(skillId);
            if (legacyType != null) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new com.etema.ragnarmmo.system.stats.net.ClientboundSkillXpPacket(legacyType, (int) amount));
            }
        }

        return levelsGained;
    }

    // ========================================================================
    // Legacy SkillType-based methods (delegate to ResourceLocation methods)
    // ========================================================================

    /**
     * Gets progress for a specific skill.
     *
     * @deprecated Use {@link #getSkillState(ResourceLocation)} instead
     */
    @Deprecated
    @SuppressWarnings("removal")
    public SkillProgress getSkill(SkillType type) {
        if (type == null)
            return null;
        ResourceLocation id = type.toResourceLocation();
        SkillState state = skills.get(id);
        return state != null ? new SkillProgress(id, state) : new SkillProgress(id);
    }

    /**
     * Gets the level of a specific skill.
     */
    @SuppressWarnings("removal")
    public int getSkillLevel(SkillType type) {
        if (type == null)
            return 0;
        return getSkillLevel(type.toResourceLocation());
    }

    @SuppressWarnings("removal")
    public double getSkillXp(SkillType type) {
        if (type == null)
            return 0;
        return getSkillXp(type.toResourceLocation());
    }

    /**
     * Adds XP to a skill.
     *
     * @return number of levels gained
     */
    @SuppressWarnings("removal")
    public int addXP(SkillType type, double amount, XPGainReason reason) {
        if (type == null)
            return 0;
        return addXP(type.toResourceLocation(), amount, reason);
    }

    @SuppressWarnings("removal")
    public int addSkillXP(SkillType type, double amount, ChangeReason reason) {
        if (type == null)
            return 0;
        return addSkillXP(type.toResourceLocation(), amount, reason);
    }

    @SuppressWarnings("removal")
    public int setSkillLevel(SkillType type, int level, ChangeReason reason) {
        if (type == null)
            return 0;
        return setSkillLevel(type.toResourceLocation(), level, reason);
    }

    /**
     * Called when a skill levels up.
     */
    protected void onLevelUp(ResourceLocation skillId, int newLevel, int levelsGained) {
        if (player instanceof ServerPlayer serverPlayer) {
            SkillType legacyType = SkillType.fromResourceLocation(skillId);
            if (legacyType != null) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new com.etema.ragnarmmo.system.stats.net.ClientboundLevelUpPacket(legacyType, newLevel));
            }
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
     * Gets all skills in a specific category.
     *
     * @deprecated Use ResourceLocation-based API with SkillRegistry.getByCategory()
     */
    @Deprecated
    @SuppressWarnings("removal")
    public Map<SkillType, SkillProgress> getSkillsByCategory(SkillCategory category) {
        Map<SkillType, SkillProgress> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, SkillState> entry : skills.entrySet()) {
            SkillType type = SkillType.fromResourceLocation(entry.getKey());
            if (type != null) {
                // Check category via SkillRegistry instead of removed SkillType.getCategory()
                SkillCategory skillCat = SkillRegistry.get(entry.getKey())
                        .map(ISkillDefinition::getCategory)
                        .orElse(null);
                if (skillCat == category) {
                    result.put(type, new SkillProgress(entry.getKey(), entry.getValue()));
                }
            }
        }
        return result;
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
        if (state == null)
            return false;

        Optional<ISkillDefinition> defOpt = SkillRegistry.get(skillId).map(d -> (ISkillDefinition) d);
        if (defOpt.isEmpty())
            return false;

        ISkillDefinition def = defOpt.get();

        // Check if skill can be upgraded with skill points
        if (!def.canUpgradeWithPoints())
            return false;

        // Check max level
        if (state.getLevel() >= def.getMaxLevel())
            return false;

        // 1. Get Stats for Skill Points
        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty())
            return false;
        var stats = statsOpt.get();

        if (stats.getSkillPoints() <= 0)
            return false;

        // 2. Check Job Allow
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(stats.getJobId());
        Set<String> allowedJobs = def.getAllowedJobs();
        
        // If the skill has restricted jobs, check if current job (or ancestor) is allowed.
        // We check:
        // 1. If allowedJobs is empty (available to all)
        // 2. If it contains the exact job ID (case-insensitive)
        // 3. If it contains the first-class ancestor ID (so Wizards can upgrade Mage skills)
        if (!allowedJobs.isEmpty()) {
            String currentJobId = job.getId().toUpperCase(Locale.ROOT);
            var firstClass = job.getFirstClassAncestor();
            String ancestorId = firstClass != null ? firstClass.getId().toUpperCase(Locale.ROOT) : "";

            boolean jobAllowed = allowedJobs.stream()
                .anyMatch(s -> s.equalsIgnoreCase(currentJobId) || s.equalsIgnoreCase(ancestorId) || s.equalsIgnoreCase("NOVICE"));

            if (!jobAllowed) {
                return false;
            }
        }

        // 3. Check Dependencies
        Map<ResourceLocation, Integer> reqs = def.getRequirements();
        for (var entry : reqs.entrySet()) {
            if (getSkillLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }

        // 4. Get upgrade cost
        int upgradeCost = def.getUpgradeCost();
        if (stats.getSkillPoints() < upgradeCost)
            return false;

        // 5. Upgrade
        boolean upgraded = state.upgradeLevel(def.getMaxLevel());

        if (upgraded) {
            stats.setSkillPoints(stats.getSkillPoints() - upgradeCost);
            onLevelUp(skillId, state.getLevel(), 1);

            // Sync stats (skill points) and skills to client
            if (player instanceof ServerPlayer sp) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(sp,
                        new com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket(stats));
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(sp,
                        new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(this.serializeNBT()));
            }
            return true;
        }

        return false;
    }

    /**
     * Attempts to upgrade a skill using a skill point (legacy).
     *
     * @deprecated Use {@link #tryUpgradeSkill(ResourceLocation)} instead
     */
    @Deprecated
    @SuppressWarnings("removal")
    public boolean tryUpgradeSkill(SkillType type) {
        if (type == null)
            return false;
        return tryUpgradeSkill(type.toResourceLocation());
    }

    // === Hotbar Management ===
    private final String[] hotbar = new String[9];

    // === Casting System ===
    private ResourceLocation activeCastSkillId;
    private int castTicksRemaining;
    private int castTotalTicks;

    public void startCast(ResourceLocation skillId, int duration) {
        this.activeCastSkillId = skillId;
        this.castTotalTicks = duration;
        this.castTicksRemaining = duration;

        if (player instanceof ServerPlayer serverPlayer) {
            String skillIdStr = skillId != null ? skillId.toString() : "";
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.skill.net.ClientboundCastUpdatePacket(skillIdStr, duration,
                            duration));
        }
    }

    /**
     * @deprecated Use {@link #startCast(ResourceLocation, int)} instead
     */
    @Deprecated
    @SuppressWarnings("removal")
    public void startCast(SkillType skill, int duration) {
        startCast(skill != null ? skill.toResourceLocation() : null, duration);
    }

    public void interruptCast() {
        if (activeCastSkillId != null) {
            this.activeCastSkillId = null;
            this.castTicksRemaining = 0;
            this.castTotalTicks = 0;

            if (player instanceof ServerPlayer serverPlayer) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new com.etema.ragnarmmo.skill.net.ClientboundCastUpdatePacket("", 0, 0));
            }
        }
    }

    public boolean isCasting() {
        return activeCastSkillId != null;
    }

    public ResourceLocation getActiveCastSkillId() {
        return activeCastSkillId;
    }

    /**
     * @deprecated Use {@link #getActiveCastSkillId()} instead
     */
    @Deprecated
    public SkillType getActiveCastSkill() {
        return activeCastSkillId != null ? SkillType.fromResourceLocation(activeCastSkillId) : null;
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
        if (slot >= 0 && slot < 9) {
            hotbar[slot] = skillId;
            if (player instanceof ServerPlayer serverPlayer) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(serializeNBT()));
            }
        }
    }

    /**
     * Serializes all skills to NBT.
     * Uses full ResourceLocation format (e.g., "ragnarmmo:bash") for future
     * compatibility.
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

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
        }

        return tag;
    }

    /**
     * Deserializes all skills from NBT.
     * Supports both legacy format ("bash") and new format ("ragnarmmo:bash").
     */
    public void deserializeNBT(CompoundTag tag) {
        // First, reset all skills to ensure clean state
        for (SkillState state : skills.values()) {
            state.reset();
        }

        // Load skills from NBT
        for (String key : tag.getAllKeys()) {
            // Skip non-skill keys
            if (key.equals("cartInventory") || key.equals("hotbar") || key.equals("cooldowns")) {
                continue;
            }

            // Parse skill ID with legacy fallback
            ResourceLocation skillId;
            if (key.contains(":")) {
                // New format: "ragnarmmo:bash"
                skillId = ResourceLocation.tryParse(key);
            } else {
                // Legacy format: "bash" -> "ragnarmmo:bash"
                skillId = new ResourceLocation(DEFAULT_NAMESPACE, key.toLowerCase());
            }

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

        // Load cooldowns with legacy support
        if (tag.contains("cooldowns")) {
            CompoundTag cooldownTag = tag.getCompound("cooldowns");
            for (String key : cooldownTag.getAllKeys()) {
                ResourceLocation skillId;
                if (key.contains(":")) {
                    skillId = ResourceLocation.tryParse(key);
                } else {
                    skillId = new ResourceLocation(DEFAULT_NAMESPACE, key.toLowerCase());
                }

                if (skillId != null) {
                    pendingCooldowns.put(skillId, cooldownTag.getLong(key));
                }
            }
        }
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

        for (int i = 0; i < cartInventory.getSlots(); i++) {
            cartInventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                    new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(serializeNBT()));
            SkillEffectHandler.refreshPassiveEffects(serverPlayer);
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
        return player != null && player.level().getGameTime() < cooldowns.getOrDefault(skillId, 0L);
    }

    /**
     * @deprecated Use {@link #isOnCooldown(ResourceLocation)} instead
     */
    @Deprecated
    @SuppressWarnings("removal")
    public boolean isOnCooldown(SkillType type) {
        return type != null && isOnCooldown(type.toResourceLocation());
    }

    public void setCooldown(ResourceLocation skillId, int ticks) {
        if (player != null && skillId != null) {
            cooldowns.put(skillId, player.level().getGameTime() + ticks);
            cooldownDurations.put(skillId, ticks);
        }
    }

    /**
     * @deprecated Use {@link #setCooldown(ResourceLocation, int)} instead
     */
    @Deprecated
    @SuppressWarnings("removal")
    public void setCooldown(SkillType type, int ticks) {
        if (type != null) {
            setCooldown(type.toResourceLocation(), ticks);
        }
    }

    public float getCooldownProgress(ResourceLocation skillId, float partialTick) {
        if (player == null || skillId == null)
            return 0f;

        long now = player.level().getGameTime();

        // Check local cooldown
        long localEnd = cooldowns.getOrDefault(skillId, 0L);
        int localDuration = cooldownDurations.getOrDefault(skillId, 1);
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
        return Math.max(localProgress, globalProgress);
    }

    /**
     * @deprecated Use {@link #getCooldownProgress(ResourceLocation, float)} instead
     */
    @Deprecated
    public float getCooldownProgress(SkillType type, float partialTick) {
        return type != null ? getCooldownProgress(type.toResourceLocation(), partialTick) : 0f;
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
}
