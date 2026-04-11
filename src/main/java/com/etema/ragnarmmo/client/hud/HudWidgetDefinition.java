package com.etema.ragnarmmo.client.hud;

import net.minecraft.network.chat.Component;

/**
 * Static metadata for one HUD widget exposed to the editor/runtime layout layer.
 */
public record HudWidgetDefinition(
        String id,
        Component displayName,
        int defaultZOrder) {
}
