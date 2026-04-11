package com.etema.ragnarmmo.client.hud;

/**
 * Runtime layout state for one HUD widget.
 */
public record HudWidgetState(
        boolean enabled,
        double anchorX,
        double anchorY,
        double scale,
        int backgroundAlpha,
        boolean showBackground,
        int zOrder) {
}
