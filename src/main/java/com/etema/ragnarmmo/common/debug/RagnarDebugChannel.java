package com.etema.ragnarmmo.common.debug;

public enum RagnarDebugChannel {
    MASTER("master"),
    COMBAT("combat"),
    PLAYER_DATA("player"),
    MOB_SPAWNS("mobs"),
    BOSS_WORLD("bosses"),
    MIGRATION("migration");

    private final String commandName;

    RagnarDebugChannel(String commandName) {
        this.commandName = commandName;
    }

    public String commandName() {
        return commandName;
    }
}
