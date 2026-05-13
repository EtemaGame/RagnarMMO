package com.etema.ragnarmmo.skills.xp;

public final class SkillXpConfig {
    private static final int DEFAULT_COMBAT_FALLBACK_XP = 10;

    private SkillXpConfig() {
    }

    public static int combatFallbackXp() {
        return DEFAULT_COMBAT_FALLBACK_XP;
    }
}
