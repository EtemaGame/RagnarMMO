package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.entity.mob.PupaEntity;

public final class PupaModel extends AbstractMobModel<PupaEntity> {
    public PupaModel() {
        super("geo/entity/pupa.geo.json", "textures/entity/pupa.png", "animations/entity/pupa.animation.json");
    }
}
