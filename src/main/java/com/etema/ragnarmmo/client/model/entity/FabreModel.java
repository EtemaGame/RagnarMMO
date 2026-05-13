package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.entity.mob.FabreEntity;

public final class FabreModel extends AbstractMobModel<FabreEntity> {
    public FabreModel() {
        super("geo/entity/fabre.geo.json", "textures/entity/fabre.png", "animations/entity/fabre.animation.json");
    }
}
