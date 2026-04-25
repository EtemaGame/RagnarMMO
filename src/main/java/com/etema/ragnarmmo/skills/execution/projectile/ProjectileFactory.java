package com.etema.ragnarmmo.skills.execution.projectile;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.entity.projectile.FireBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.IceBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class ProjectileFactory {

    private ProjectileFactory() {}

    /**
     * Creates a bolt-style projectile based on the given element.
     */
    public static AbstractMagicProjectile createBolt(
            ElementType type,
            Level level,
            LivingEntity owner,
            float damage
    ) {
        return switch (type) {
            case FIRE -> new FireBoltProjectile(level, owner, damage);
            case WATER -> new IceBoltProjectile(level, owner, damage);
            case WIND -> new LightningBoltProjectile(level, owner, damage);
            default -> new FireBoltProjectile(level, owner, damage); // Fallback
        };
    }
}
