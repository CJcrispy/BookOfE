package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.ShadowQuinnGoal;
import net.cjcrispy.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShadowQuinnEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    
    // Tracked data for syncing between client and server
    private static final TrackedData<String> ATTACK_STATE = DataTracker.registerData(ShadowQuinnEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> ATTACK_ANIMATION_TIMER = DataTracker.registerData(ShadowQuinnEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Shadow Quinn, The Fallen"),
            BossBar.Color.RED, BossBar.Style.NOTCHED_10);

    public ShadowQuinnEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, "");
        builder.add(ATTACK_ANIMATION_TIMER, 0);
    }

    @Override
    protected void initGoals() {
        // Attack-related goals - use custom attack goal
        this.goalSelector.add(1, new ShadowQuinnGoal(this, 1.2, false)); // Custom attack logic with multiple attack types
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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.ShadowQuinn.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.ShadowQuinn.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.ShadowQuinn.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, MobConfig.ShadowQuinn.ATTACK_KNOCKBACK)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.ShadowQuinn.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.ShadowQuinn.ARMOR)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, MobConfig.ShadowQuinn.KNOCKBACK_RESISTANCE)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.ShadowQuinn.SCALE);
    }


    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
            // Timer is synced from server via DataTracker
        } else {
            // Server-side behavior - update attack animation timer
            int timer = this.dataTracker.get(ATTACK_ANIMATION_TIMER);
            if (timer > 0) {
                this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer - 1);
                if (timer - 1 <= 0) {
                    this.dataTracker.set(ATTACK_STATE, "");
                }
            }
            
            // Prevent movement interruptions during attack animations
            String state = this.dataTracker.get(ATTACK_STATE);
            if (!state.isEmpty() && timer > 0) {
                // Only allow minimal movement during spin attack, stop completely for others
                if (!"spinattack".equals(state)) {
                    this.getNavigation().stop();
                }
            }
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        this.dropItem(ModItems.CALAMITY);
    }


    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        // Grant advancement to all players who participated in the fight
        if (!this.getWorld().isClient() && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            // Ensure root advancement is granted first
            net.minecraft.advancement.AdvancementEntry rootAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "root"));
            net.minecraft.advancement.AdvancementEntry killAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_shadow_quinn"));
            
            // Collect all participating players
            java.util.Set<net.minecraft.server.network.ServerPlayerEntity> participants = new java.util.HashSet<>();
            participants.addAll(this.bossBar.getPlayers());
            
            // Also add nearby players within 100 blocks
            net.minecraft.util.math.Box searchBox = this.getBoundingBox().expand(100.0);
            for (PlayerEntity player : serverWorld.getPlayers(player -> 
                    player.isAlive() && searchBox.intersects(player.getBoundingBox()))) {
                if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    participants.add(serverPlayer);
                }
            }
            
            // Grant root advancement first, then kill advancement
            for (net.minecraft.server.network.ServerPlayerEntity player : participants) {
                net.minecraft.advancement.PlayerAdvancementTracker tracker = player.getAdvancementTracker();
                
                // Grant root if not already granted
                if (rootAdvancement != null && !tracker.getProgress(rootAdvancement).isDone()) {
                    tracker.grantCriterion(rootAdvancement, "requirement");
                }
                
                // Grant kill advancement
                if (killAdvancement != null && !tracker.getProgress(killAdvancement).isDone()) {
                    tracker.grantCriterion(killAdvancement, "killed_shadow_quinn");
                }
            }
        }

        // Example: Add a death sound or custom effects
        this.getWorld().sendEntityStatus(this, (byte) 60); // Play the death particles
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // Use transition length of 0 for immediate animation changes
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate)
                .setAnimationSpeed(1.0f)); // Ensure normal animation speed
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        // If the entity is dead, play the death animation and stop further updates
        if (this.isDead() || !this.isAlive()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("death", Animation.LoopType.PLAY_ONCE));
            return PlayState.STOP; // Stop further animation updates
        }

        // Handle attack animations - check timer FIRST (priority over movement/idle)
        // This is the "slime trick" - always set animation while timer is active
        int timer = this.dataTracker.get(ATTACK_ANIMATION_TIMER);
        String attackState = this.dataTracker.get(ATTACK_STATE);
        
        if (timer > 0 && !attackState.isEmpty()) {
            RawAnimation animation = null;
            switch (attackState) {
                case "attack":
                    animation = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
                    break;
                case "chargedattack":
                    animation = RawAnimation.begin().then("chargedattack", Animation.LoopType.PLAY_ONCE);
                    break;
                case "spinattack":
                    animation = RawAnimation.begin().then("spinattack", Animation.LoopType.PLAY_ONCE);
                    break;
            }
            if (animation != null) {
                // Always set the animation while timer is active - this ensures it plays to completion
                tAnimationState.getController().setAnimation(animation);
            }
            return PlayState.CONTINUE;
        }

        // Fallback to hand swinging for regular attacks if attack state is not set
        if (this.handSwinging && attackState.isEmpty()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        // Handle movement and idle animations - only if not in an attack animation
        if (timer <= 0) {
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

        return PlayState.CONTINUE;
    }

    // Getter and setter for attack state
    public String getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }

    public void setAttackState(String state) {
        this.dataTracker.set(ATTACK_STATE, state);
        // Set animation timer based on attack type
        // Timer is decremented on both client and server
        int timer = 0;
        switch (state) {
            case "attack":
                timer = (int) Math.ceil(1.4 * 20); // 1.4 seconds * 20 ticks
                break;
            case "chargedattack":
                timer = (int) Math.ceil(2.72 * 20); // 2.72 seconds * 20 ticks
                break;
            case "spinattack":
                timer = (int) Math.ceil(2.56 * 20); // 2.56 seconds * 20 ticks
                break;
            default:
                timer = 0;
        }
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer);
    }


    public boolean isAggro() {
        return this.getTarget() != null; // Aggro if the entity has a valid target
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("AttackState", this.dataTracker.get(ATTACK_STATE));
        nbt.putInt("AttackAnimationTimer", this.dataTracker.get(ATTACK_ANIMATION_TIMER));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(ATTACK_STATE, nbt.getString("AttackState"));
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, nbt.getInt("AttackAnimationTimer"));
    }

    /* BOSS BAR */
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

}
