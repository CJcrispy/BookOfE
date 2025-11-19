package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BlackBirdEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean shouldExplodeOnDeath = false;
    private int attackAnimationTimer = 0;

    public BlackBirdEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }
    
    public void setShouldExplodeOnDeath(boolean shouldExplode) {
        this.shouldExplodeOnDeath = shouldExplode;
    }
    
    public boolean shouldExplodeOnDeath() {
        return this.shouldExplodeOnDeath;
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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.BlackBird.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.BlackBird.ARMOR)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.BlackBird.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.BlackBird.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.BlackBird.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.BlackBird.SCALE);
    }


    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
            // Decrement attack animation timer
            if (attackAnimationTimer > 0) {
                attackAnimationTimer--;
            }
        } else {
            // Server-side behavior
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        // Handle explosion before calling super.onDeath()
        if (this.shouldExplodeOnDeath && !this.getWorld().isClient() && this.getWorld() instanceof ServerWorld serverWorld) {
            // Create a small explosion on death
            serverWorld.createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.0F, World.ExplosionSourceType.MOB);
        }
        
        super.onDeath(damageSource);

        // Example: Add a death sound or custom effects
        this.getWorld().sendEntityStatus(this, (byte) 60); // Play the death particles
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("ShouldExplodeOnDeath", this.shouldExplodeOnDeath);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.shouldExplodeOnDeath = nbt.getBoolean("ShouldExplodeOnDeath");
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        // If the entity is dead, play the death animation and stop further updates
        if (this.isDead()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("death", Animation.LoopType.PLAY_ONCE));
            return PlayState.STOP; // Stop further animation updates
        }

        // Check if attack animation is currently playing - let it finish
        // Attack animation typically lasts about 20 ticks (1 second), so we'll use a timer
        if (attackAnimationTimer > 0) {
            // Keep the attack animation playing
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        // Check if entity is attacking - start the attack animation
        if (this.handSwinging) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
            attackAnimationTimer = 20; // Set timer to allow animation to play for ~1 second
            return PlayState.CONTINUE;
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
