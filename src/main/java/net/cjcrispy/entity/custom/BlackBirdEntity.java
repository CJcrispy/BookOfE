package net.cjcrispy.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BlackBirdEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean isDying = false;

    public BlackBirdEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }


    @Override
    protected void initGoals() {
        // Attack-related goals
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false)); // Attack logic
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, VillagerEntity.class, true)); // Target villagers proactively
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, VillagerEntity.class, 8.0F)); // Look at villagers
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10F)
                .add(EntityAttributes.GENERIC_ARMOR, 0F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 10)
                .add(EntityAttributes.GENERIC_SCALE, 1.35);
    }


    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
        } else {
            // Server-side behavior
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (!this.isDying) {
            this.isDying = true;
            this.getWorld().sendEntityStatus(this, (byte) 60); // Triggers animation
        }
        super.onDeath(damageSource);
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 60) {
            this.isDying = true;
        }
        super.handleStatus(status);
    }

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");

    private final AnimationController ATTACK_ANIMATION_CONTROLLER = new AnimationController<>(this, "attack_controller", state -> PlayState.STOP)
            .triggerableAnim("attack_animation", ATTACK_ANIMATION).transitionLength(5);


    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
        controllers.add(
                DefaultAnimations.genericWalkIdleController(this).transitionLength(5),
                ATTACK_ANIMATION_CONTROLLER,
                DefaultAnimations.genericLivingController(this)
        );
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        // If the entity is dead, play the death animation and stop further updates
        if (this.isDying) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("death", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE; // Stop further animation updates
        }

        if (tAnimationState.isMoving()) {
            tAnimationState.getController().setAnimation(
                    RawAnimation.begin().then("walk", Animation.LoopType.LOOP)
            );
            return PlayState.CONTINUE;
        } else {
            tAnimationState.getController().setAnimation(
                    RawAnimation.begin().then("idle", Animation.LoopType.LOOP)
            );
            return PlayState.CONTINUE;
        }
    }

    public boolean isAggro() {
        return this.getTarget() != null; // Aggro if the entity has a valid target
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
