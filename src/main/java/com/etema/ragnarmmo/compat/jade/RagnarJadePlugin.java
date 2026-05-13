package com.etema.ragnarmmo.compat.jade;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(RagnarMMO.MODID)
public class RagnarJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(RagnarMobJadeProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(RagnarMobJadeProvider.INSTANCE, LivingEntity.class);
    }
}
