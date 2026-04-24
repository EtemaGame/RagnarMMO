package com.etema.ragnarmmo.client;

import net.minecraft.resources.ResourceLocation;

public class ClientCastManager {
    private static final ClientCastManager INSTANCE = new ClientCastManager();

    private ResourceLocation castingSkillId;
    private int currentTicks;
    private int totalTicks;

    public static ClientCastManager getInstance() {
        return INSTANCE;
    }

    public void updateCast(ResourceLocation skillId, int current, int total) {
        this.castingSkillId = skillId;
        this.currentTicks = current;
        this.totalTicks = total;
    }

    public void tick() {
        if (currentTicks > 0) {
            currentTicks--;
            if (currentTicks <= 0) {
                this.castingSkillId = null;
            }
        }
    }

    public boolean isCasting() {
        return castingSkillId != null && currentTicks > 0;
    }

    public ResourceLocation getCastingSkillId() {
        return castingSkillId;
    }

    public float getProgress() {
        if (totalTicks <= 0)
            return 0f;
        return 1.0f - ((float) currentTicks / (float) totalTicks);
    }

    public int getCurrentTicks() {
        return currentTicks;
    }

    public int getTotalTicks() {
        return totalTicks;
    }
}
