package com.etema.ragnarmmo.common.api.stats;

/**
 * Describes why a player data mutation happened.
 * Used to route changes through safe APIs and enable future auditing/logging.
 */
public enum ChangeReason {
    PLAYER_ACTION,
    ADMIN_COMMAND,
    DEBUG,
    SYSTEM
}

