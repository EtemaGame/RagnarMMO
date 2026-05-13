package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.entity.mob.MukaEntity;

public final class MukaModel extends AbstractMobModel<MukaEntity> {
    public MukaModel() {
        super("geo/entity/muka.geo.json", "textures/entity/muka.png", "animations/entity/muka.animation.json");
    }
}
