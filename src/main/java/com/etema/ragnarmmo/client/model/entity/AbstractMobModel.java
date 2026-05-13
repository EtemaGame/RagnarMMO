package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * Shared fallback model plumbing for Ragnarok-style mobs.
 * Each concrete mob keeps its own resource paths, but falls back to the
 * Poring assets while those mob-specific assets are still being built.
 */
public abstract class AbstractMobModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation FALLBACK_MODEL = id("geo/entity/poring.geo.json");
    private static final ResourceLocation FALLBACK_TEXTURE = id("textures/entity/poring.png");
    private static final ResourceLocation FALLBACK_ANIMATION = id("animations/entity/poring.animation.json");

    private final String modelPath;
    private final String texturePath;
    private final String animationPath;

    protected AbstractMobModel(String modelPath, String texturePath) {
        this(modelPath, texturePath, null);
    }

    protected AbstractMobModel(String modelPath, String texturePath, String animationPath) {
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return resolve(modelPath, FALLBACK_MODEL);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return resolve(texturePath, FALLBACK_TEXTURE);
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animationPath == null ? FALLBACK_ANIMATION : resolve(animationPath, FALLBACK_ANIMATION);
    }

    private static ResourceLocation resolve(String path, ResourceLocation fallback) {
        ResourceLocation resource = id(path);
        return Minecraft.getInstance().getResourceManager().getResource(resource).isPresent()
                ? resource
                : fallback;
    }

    protected static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, path);
    }
}
