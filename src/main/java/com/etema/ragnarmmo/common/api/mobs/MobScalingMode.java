package com.etema.ragnarmmo.common.api.mobs;

/**
 * Semantic level-resolution mode for the new mob system.
 *
 * <p>The values are mutually exclusive and do not imply multi-mode blending.</p>
 */
public enum MobScalingMode {
    DISTANCE,
    PLAYER_LEVEL,
    MANUAL
}
