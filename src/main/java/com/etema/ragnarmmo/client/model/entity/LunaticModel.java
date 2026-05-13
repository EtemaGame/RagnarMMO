package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.entity.mob.LunaticEntity;

public final class LunaticModel extends AbstractMobModel<LunaticEntity> {
    public LunaticModel() {
        super("geo/entity/lunatic.geo.json", "textures/entity/lunatic.png", "animations/entity/lunatic.animation.json");
    }
}
