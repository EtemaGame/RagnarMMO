package com.etema.ragnarmmo.achievements.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerAchievements implements IPlayerAchievements {

    private final Set<String> unlockedAchievements = new HashSet<>();
    private final Set<String> claimedRewards = new HashSet<>();
    private final Map<String, Integer> progressTrackers = new HashMap<>();

    private int totalPoints = 0;
    private String activeTitle = null;
    private boolean isDirty = false;

    @Override
    public Set<String> getUnlockedAchievements() {
        return unlockedAchievements;
    }

    @Override
    public Set<String> getClaimedRewards() {
        return claimedRewards;
    }

    @Override
    public int getTotalPoints() {
        return totalPoints;
    }

    @Override
    public String getActiveTitle() {
        return activeTitle;
    }

    @Override
    public void setActiveTitle(String title) {
        if (title == null || title.isEmpty()) {
            this.activeTitle = null;
        } else {
            this.activeTitle = title;
        }
        markDirty();
    }

    @Override
    public void unlockAchievement(String id, int pointsAwarded) {
        if (unlockedAchievements.add(id)) {
            totalPoints += pointsAwarded;
            markDirty();
        }
    }

    @Override
    public void claimReward(String id) {
        if (claimedRewards.add(id)) {
            markDirty();
        }
    }

    @Override
    public boolean isUnlocked(String id) {
        return unlockedAchievements.contains(id);
    }

    @Override
    public boolean isClaimed(String id) {
        return claimedRewards.contains(id);
    }

    @Override
    public void markDirty() {
        this.isDirty = true;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public void clearDirty() {
        this.isDirty = false;
    }

    @Override
    public int getProgress(String id) {
        return progressTrackers.getOrDefault(id, 0);
    }

    @Override
    public void addProgress(String id, int amount) {
        progressTrackers.put(id, getProgress(id) + amount);
        markDirty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag unlockedList = new ListTag();
        unlockedAchievements.forEach(id -> unlockedList.add(StringTag.valueOf(id)));
        tag.put("Unlocked", unlockedList);

        ListTag claimedList = new ListTag();
        claimedRewards.forEach(id -> claimedList.add(StringTag.valueOf(id)));
        tag.put("Claimed", claimedList);

        CompoundTag progressTag = new CompoundTag();
        progressTrackers.forEach(progressTag::putInt);
        tag.put("Progress", progressTag);

        tag.putInt("Points", totalPoints);

        if (activeTitle != null) {
            tag.putString("ActiveTitle", activeTitle);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        unlockedAchievements.clear();
        if (tag.contains("Unlocked", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Unlocked", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                unlockedAchievements.add(list.getString(i));
            }
        }

        claimedRewards.clear();
        if (tag.contains("Claimed", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Claimed", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                claimedRewards.add(list.getString(i));
            }
        }

        progressTrackers.clear();
        if (tag.contains("Progress", Tag.TAG_COMPOUND)) {
            CompoundTag progressTag = tag.getCompound("Progress");
            for (String key : progressTag.getAllKeys()) {
                progressTrackers.put(key, progressTag.getInt(key));
            }
        }

        totalPoints = tag.getInt("Points");

        if (tag.contains("ActiveTitle", Tag.TAG_STRING)) {
            activeTitle = tag.getString("ActiveTitle");
        } else {
            activeTitle = null;
        }
    }
}
