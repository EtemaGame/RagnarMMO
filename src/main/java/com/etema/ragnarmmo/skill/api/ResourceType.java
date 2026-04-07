package com.etema.ragnarmmo.skill.api;

public enum ResourceType {
    SP,
    MANA,
    COOLDOWN_ONLY;

    public boolean isResource() {
        return this != COOLDOWN_ONLY;
    }
}
