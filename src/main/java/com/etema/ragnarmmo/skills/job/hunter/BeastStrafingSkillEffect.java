package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.execution.projectile.RagnarArrowSpawnHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * Beast Strafing — Passive
 * RO: Fires two additional arrows when attacking a Brute monster.
 *     In higher RO versions: rapid-fires against beast/insect type enemies.
 *
 * Minecraft:
 *  - execute: fires additional contract-snapshotted arrows without mutating hurt-event damage.
 *  - Level scales arrow damage bonus per extra arrow.
 */
public class BeastStrafingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "beast_strafing");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        double baseAtk = com.etema.ragnarmmo.common.api.stats.StatAttributes.getTotal(player, com.etema.ragnarmmo.common.api.stats.StatKeys.DEX);
        float bonusDmg = (float) (baseAtk * (1.2f + level * 0.1f));
        int extraArrows = Math.min(level / 3 + 1, 3);

        for (int i = 0; i < extraArrows; i++) {
            double spread = 0.05 * (i + 1);
            Vec3 look = player.getLookAngle();
            Vec3 direction = new Vec3(
                    look.x + player.getRandom().nextGaussian() * spread,
                    look.y + player.getRandom().nextGaussian() * spread,
                    look.z + player.getRandom().nextGaussian() * spread);
            RagnarArrowSpawnHelper.spawn(player, direction, 3.5F, 0.0F, 1.0F,
                    arrow -> {
                        arrow.setBaseDamage(bonusDmg);
                        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }, snapshot -> {
                        snapshot.putBoolean("bypass_iframes", true);
                    });
        }
    }
}
