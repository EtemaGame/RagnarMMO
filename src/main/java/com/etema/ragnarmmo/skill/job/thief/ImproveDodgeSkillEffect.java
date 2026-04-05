package com.etema.ragnarmmo.skill.job.thief;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class ImproveDodgeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "improve_dodge");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    // Improve Dodge: Passively increases Flee (+3 to +30) and Movement Speed for Assassins.
    // Hooked in CommonEvents/CombatMath usually, but we can use an event here too.
    
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        // This is handled in CommonEvents.onHurt / onPlayerHurt by checking skill levels
    }
}
