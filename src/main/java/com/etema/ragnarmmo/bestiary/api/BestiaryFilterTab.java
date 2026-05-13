package com.etema.ragnarmmo.bestiary.api;

public enum BestiaryFilterTab {
    ALL,
    AGGRESSIVE,
    PASSIVE,
    NEUTRAL,
    BOSS,
    UNKNOWN;

    public boolean accepts(BestiaryCategory category) {
        return this == ALL || this.name().equals(category.name());
    }
}
