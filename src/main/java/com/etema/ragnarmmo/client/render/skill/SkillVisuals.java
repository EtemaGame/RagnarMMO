package com.etema.ragnarmmo.client.render.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import java.util.Optional;

/**
 * Data record holding visual properties for a skill effect.
 */
public record SkillVisuals(
    ResourceLocation texture,
    int columns,
    int rows,
    float size,
    VisualType type,
    Optional<ResourceLocation> model,
    Optional<BlockState> blockState,
    int animationFPS,
    boolean billboard,
    RenderType renderType
) {
    public enum VisualType {
        SPRITE,
        MODEL,
        BLOCK
    }

    public static Builder builder(ResourceLocation texture) {
        return new Builder(texture);
    }

    public static class Builder {
        private final ResourceLocation texture;
        private int columns = 1;
        private int rows = 1;
        private float size = 1.0f;
        private VisualType type = VisualType.SPRITE;
        private Optional<ResourceLocation> model = Optional.empty();
        private Optional<BlockState> blockState = Optional.empty();
        private int animationFPS = 20;
        private boolean billboard = true;
        private RenderType renderType = RenderType.entityCutoutNoCull(new ResourceLocation("minecraft", "missingno")); // Placeholder

        public Builder(ResourceLocation texture) {
            this.texture = texture;
            this.renderType = RenderType.entityTranslucent(texture);
        }

        public Builder columns(int columns) { this.columns = columns; return this; }
        public Builder rows(int rows) { this.rows = rows; return this; }
        public Builder size(float size) { this.size = size; return this; }
        public Builder type(VisualType type) { this.type = type; return this; }
        public Builder model(ResourceLocation model) { this.model = Optional.of(model); this.type = VisualType.MODEL; return this; }
        public Builder block(BlockState state) { this.blockState = Optional.of(state); this.type = VisualType.BLOCK; return this; }
        public Builder animationFPS(int fps) { this.animationFPS = fps; return this; }
        public Builder billboard(boolean billboard) { this.billboard = billboard; return this; }
        public Builder renderType(RenderType renderType) { this.renderType = renderType; return this; }

        public SkillVisuals build() {
            return new SkillVisuals(texture, columns, rows, size, type, model, blockState, animationFPS, billboard, renderType);
        }
    }
}
