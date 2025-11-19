package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.JoeRebelGoal;
import net.cjcrispy.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class JoeRebelEntity extends HostileEntity {
    // Store equipped armor and hand items
    private final DefaultedList<ItemStack> equippedItems = DefaultedList.ofSize(6, ItemStack.EMPTY);

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Joe, Rebel Leader"),
            BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
    
    // Tracked data for syncing between client and server
    private static final TrackedData<String> ATTACK_STATE = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> ATTACK_ANIMATION_TIMER = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_INVISIBLE_STATE = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> INVISIBILITY_TIMER = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> REBELLION_FURY_ACTIVE = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> NIGHTFALL_EXECUTION_USED = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_TIRED = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TIRED_TIMER = DataTracker.registerData(JoeRebelEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public JoeRebelEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.initEquipment(world.getRandom(), world.getLocalDifficulty(this.getBlockPos()));
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_STATE, "");
        builder.add(ATTACK_ANIMATION_TIMER, 0);
        builder.add(IS_INVISIBLE_STATE, false);
        builder.add(INVISIBILITY_TIMER, 0);
        builder.add(REBELLION_FURY_ACTIVE, false);
        builder.add(NIGHTFALL_EXECUTION_USED, false);
        builder.add(IS_TIRED, false);
        builder.add(TIRED_TIMER, 0);
    }

    @Override
    protected void initGoals() {
        // Attack-related goals - use custom attack goal
        this.goalSelector.add(1, new JoeRebelGoal(this, 1.2, false)); // Custom attack logic with multiple abilities
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, KingHajileEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MillyKnightEntity.class, true)); // Target Milly proactively
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ShadowQuinnEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, NickySummonerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, SlimeChrisEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, MillyKnightEntity.class, 8.0F)); // Look at Milly
        this.goalSelector.add(5, new LookAtEntityGoal(this, ShadowQuinnEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, SlimeChrisEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, KingHajileEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, NickySummonerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.JoeRebel.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.JoeRebel.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.JoeRebel.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, MobConfig.JoeRebel.ATTACK_KNOCKBACK)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.JoeRebel.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.JoeRebel.ARMOR)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.JoeRebel.SCALE);
    }

    /**
     * Entity tick logic (runs every tick).
     */
    @Override
    public void tick() {
        super.tick(); // Call parent tick logic
        
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
            // Update invisibility visibility
            this.setInvisible(this.dataTracker.get(IS_INVISIBLE_STATE));
        } else {
            // Server-side behavior
            // Update attack animation timer
            int timer = this.dataTracker.get(ATTACK_ANIMATION_TIMER);
            if (timer > 0) {
                this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer - 1);
                if (timer - 1 <= 0) {
                    this.dataTracker.set(ATTACK_STATE, "");
                }
            }
            
            // Update invisibility timer
            int invisTimer = this.dataTracker.get(INVISIBILITY_TIMER);
            if (invisTimer > 0) {
                this.dataTracker.set(INVISIBILITY_TIMER, invisTimer - 1);
                if (invisTimer - 1 <= 0) {
                    this.dataTracker.set(IS_INVISIBLE_STATE, false);
                    this.setInvisible(false);
                }
            }
            
            // Update tired timer
            int tiredTimer = this.dataTracker.get(TIRED_TIMER);
            if (tiredTimer > 0) {
                this.dataTracker.set(TIRED_TIMER, tiredTimer - 1);
                if (tiredTimer - 1 <= 0) {
                    this.dataTracker.set(IS_TIRED, false);
                }
            }
            
            // Check for Rebellion's Fury phase (25% HP)
            if (!this.dataTracker.get(REBELLION_FURY_ACTIVE) && this.getHealth() <= this.getMaxHealth() * 0.25) {
                this.activateRebellionFury();
            }
            
            // Show particles when Rebellion's Fury is active
            if (this.dataTracker.get(REBELLION_FURY_ACTIVE) && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                if (this.age % 5 == 0) { // Every 5 ticks
                    serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, 
                        this.getX(), this.getY() + 1, this.getZ(), 
                        3, 0.3, 0.5, 0.3, 0.1);
                    serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, 
                        this.getX(), this.getY() + 1, this.getZ(), 
                        2, 0.2, 0.3, 0.2, 0.05);
                }
            }
            
            // Prevent movement interruptions during attack animations
            String state = this.dataTracker.get(ATTACK_STATE);
            if (!state.isEmpty() && timer > 0) {
                if (!"dash".equals(state) && !"nightfall".equals(state)) {
                    this.getNavigation().stop();
                }
            }
        }
    }


    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        this.dropItem(ModItems.WORM_HAMMER);
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
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_joe_rebel"));
            
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
                    tracker.grantCriterion(killAdvancement, "killed_joe_rebel");
                }
            }
        }

        // Example: Add a death sound or custom effects
        this.getWorld().sendEntityStatus(this, (byte) 60); // Play the death particles
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
    
    // Getter and setter for attack state
    public String getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }
    
    public void setAttackState(String state) {
        this.dataTracker.set(ATTACK_STATE, state);
        int timer = 0;
        switch (state) {
            case "dash":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "throwing_knives":
                timer = (int) Math.ceil(1.5 * 20); // 1.5 seconds
                break;
            case "smoke_bomb":
                timer = (int) Math.ceil(2.5 * 20); // 2.5 seconds (includes invisibility)
                break;
            case "shadow_clone":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "arrow_rain":
                timer = (int) Math.ceil(3.0 * 20); // 3 seconds
                break;
            case "nightfall":
                timer = (int) Math.ceil(3.0 * 20); // 3 seconds
                break;
            case "melee":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            default:
                timer = 0;
        }
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer);
    }
    
    // Invisibility methods
    public boolean isInInvisibilityState() {
        return this.dataTracker.get(IS_INVISIBLE_STATE);
    }
    
    public void setInvisibilityState(boolean invisible, int duration) {
        this.dataTracker.set(IS_INVISIBLE_STATE, invisible);
        this.dataTracker.set(INVISIBILITY_TIMER, duration);
        this.setInvisible(invisible);
    }
    
    // Rebellion's Fury methods
    public boolean isRebellionFuryActive() {
        return this.dataTracker.get(REBELLION_FURY_ACTIVE);
    }
    
    private void activateRebellionFury() {
        this.dataTracker.set(REBELLION_FURY_ACTIVE, true);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, false, false)); // Speed II
        // Say dialogue line
        if (this.getTarget() != null) {
            this.sendMessage(Text.literal("You think the Rebellion dies with me?"));
        }
    }
    
    // Nightfall Execution methods
    public boolean hasUsedNightfallExecution() {
        return this.dataTracker.get(NIGHTFALL_EXECUTION_USED);
    }
    
    public void setNightfallExecutionUsed() {
        this.dataTracker.set(NIGHTFALL_EXECUTION_USED, true);
    }
    
    // Tired state methods
    public boolean isTired() {
        return this.dataTracker.get(IS_TIRED);
    }
    
    public void setTired(int duration) {
        this.dataTracker.set(IS_TIRED, true);
        this.dataTracker.set(TIRED_TIMER, duration);
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Apply tired state damage multiplier
        if (this.isTired()) {
            amount *= 1.5f; // +50% damage
        }
        
        return super.damage(source, amount);
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("AttackState", this.dataTracker.get(ATTACK_STATE));
        nbt.putInt("AttackAnimationTimer", this.dataTracker.get(ATTACK_ANIMATION_TIMER));
        nbt.putBoolean("RebellionFuryActive", this.dataTracker.get(REBELLION_FURY_ACTIVE));
        nbt.putBoolean("NightfallExecutionUsed", this.dataTracker.get(NIGHTFALL_EXECUTION_USED));
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(ATTACK_STATE, nbt.getString("AttackState"));
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, nbt.getInt("AttackAnimationTimer"));
        this.dataTracker.set(REBELLION_FURY_ACTIVE, nbt.getBoolean("RebellionFuryActive"));
        this.dataTracker.set(NIGHTFALL_EXECUTION_USED, nbt.getBoolean("NightfallExecutionUsed"));
    }
}
