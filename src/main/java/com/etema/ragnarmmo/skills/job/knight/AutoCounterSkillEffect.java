package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

/**
 * Auto Counter — Passive (Counter-attack on being hit)
 * RO: When an enemy attacks, there is a chance to perform a critical counter-attack.
 *     At level 5, 100% critical hit chance on the counter. Requires melee weapon.
 *     Counter proc chance: 20% * level.
 *
 * Minecraft:
 *  - On defensive hurt, has a (10% × level) chance to immediately strike back.
 *  - The counter-attack deals 150% of current weapon damage and is a guaranteed crit.
 *  - Counter is tracked via PersistentData flag to avoid recursive triggers.
 *  - Requires 2H sword or spear held (Knight weapons).
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AutoCounterSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "auto_counter");
    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
