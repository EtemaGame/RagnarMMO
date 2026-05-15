package com.etema.ragnarmmo.entity.mob;

import com.etema.ragnarmmo.common.init.RagnarEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AbstractRagnarMobEntity extends PathfinderMob implements GeoEntity {
    protected static final RawAnimation PORING_IDLE = RawAnimation.begin().thenLoop("animation.poring.idle");
    protected static final RawAnimation PORING_WALK = RawAnimation.begin().thenLoop("animation.poring.walk");
    protected static final RawAnimation PORING_ATTACK = RawAnimation.begin().thenLoop("animation.poring.attack");

    protected static final RawAnimation LUNATIC_IDLE = RawAnimation.begin().thenLoop("animation.ragnarmmo.lunatic.idle");
    protected static final RawAnimation LUNATIC_WALK = RawAnimation.begin().thenLoop("animation.ragnarmmo.lunatic.walk");
    protected static final RawAnimation LUNATIC_ATTACK = RawAnimation.begin().thenLoop("animation.ragnarmmo.lunatic.attack");

    protected static final RawAnimation FABRE_IDLE = RawAnimation.begin().thenLoop("animation.ragnarmmo.fabre.idle");
    protected static final RawAnimation FABRE_WALK = RawAnimation.begin().thenLoop("animation.ragnarmmo.fabre.walk");
    protected static final RawAnimation FABRE_ATTACK = RawAnimation.begin().thenLoop("animation.ragnarmmo.fabre.attack");

    protected static final RawAnimation MUKA_IDLE = RawAnimation.begin().thenLoop("animation.ragnarmmo.muka.idle");
    protected static final RawAnimation MUKA_WALK = RawAnimation.begin().thenLoop("animation.ragnarmmo.muka.walk");
    protected static final RawAnimation MUKA_ATTACK = RawAnimation.begin().thenLoop("animation.ragnarmmo.muka.attack");

    protected static final RawAnimation PUPA_IDLE = RawAnimation.begin().thenLoop("animation.ragnarmmo.pupa.idle");

    protected static final RawAnimation CREAMY_IDLE = RawAnimation.begin().thenLoop("walk/stading");
    protected static final RawAnimation CREAMY_WALK = RawAnimation.begin().thenLoop("walk/stading");
    protected static final RawAnimation CREAMY_ATTACK = RawAnimation.begin().thenLoop("attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractRagnarMobEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> state.setAndContinue(animationForType())));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    protected RawAnimation animationForType() {
        EntityType<?> type = this.getType();
        if (type == RagnarEntities.PORING.get()
                || type == RagnarEntities.POPORING.get()
                || type == RagnarEntities.DROP.get()
                || type == RagnarEntities.MARIN.get()) {
            return animationForPoringFamily();
        }
        if (type == RagnarEntities.LUNATIC.get()) {
            return animationForLunatic();
        }
        if (type == RagnarEntities.FABRE.get()) {
            return animationForFabre();
        }
        if (type == RagnarEntities.MUKA.get()) {
            return animationForMuka();
        }
        if (type == RagnarEntities.PUPA.get()) {
            return animationForPupa();
        }
        if (type == RagnarEntities.CREAMY.get() || type == RagnarEntities.CREAMY_FEAR.get()) {
            return animationForCreamy();
        }
        return PORING_IDLE;
    }

    protected RawAnimation animationForPoringFamily() {
        return selectStatefulAnimation(PORING_IDLE, PORING_WALK, PORING_ATTACK);
    }

    protected RawAnimation animationForLunatic() {
        return selectStatefulAnimation(LUNATIC_IDLE, LUNATIC_WALK, LUNATIC_ATTACK);
    }

    protected RawAnimation animationForFabre() {
        return selectStatefulAnimation(FABRE_IDLE, FABRE_WALK, FABRE_ATTACK);
    }

    protected RawAnimation animationForMuka() {
        return selectStatefulAnimation(MUKA_IDLE, MUKA_WALK, MUKA_ATTACK);
    }

    protected RawAnimation animationForPupa() {
        return PUPA_IDLE;
    }

    protected RawAnimation animationForCreamy() {
        return selectStatefulAnimation(CREAMY_IDLE, CREAMY_WALK, CREAMY_ATTACK);
    }

    protected RawAnimation selectStatefulAnimation(RawAnimation idle, RawAnimation walk, RawAnimation attack) {
        if (shouldPlayAttackAnimation()) {
            return attack;
        }
        if (isMoving()) {
            return walk;
        }
        return idle;
    }

    protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
    }

    protected boolean shouldPlayAttackAnimation() {
        var target = this.getTarget();
        return target != null && target.isAlive() && this.distanceToSqr(target) <= 9.0D;
    }
}
