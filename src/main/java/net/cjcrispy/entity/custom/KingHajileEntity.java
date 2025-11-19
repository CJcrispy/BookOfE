package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.KingHajileGoal;
import net.cjcrispy.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

public class KingHajileEntity extends HostileEntity {

    // Store equipped armor and hand items
    private final DefaultedList<ItemStack> equippedItems = DefaultedList.ofSize(6, ItemStack.EMPTY);

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("King Hajile"),
            BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
    
    // Phase tracking
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> ATTACK_STATE = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> ATTACK_ANIMATION_TIMER = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SANCTIFIED_WARD_ACTIVE = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SANCTIFIED_WARD_TIMER = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_IN_TRANSITION = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TRANSITION_TIMER = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> LIGHT_DEVOURER_ACTIVE = DataTracker.registerData(KingHajileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    private int totemEffectCount = 0; // Track totem effects during transition
    private double transitionAirHeight = 0.0; // Store height during transition
    private int lightningStrikeCooldown = 0;
    private static final int PHASE_1 = 1;
    private static final int PHASE_2 = 2;

    public KingHajileEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.initEquipment(world.getRandom(), world.getLocalDifficulty(this.getBlockPos()));
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PHASE, PHASE_1);
        builder.add(ATTACK_STATE, "");
        builder.add(ATTACK_ANIMATION_TIMER, 0);
        builder.add(SANCTIFIED_WARD_ACTIVE, false);
        builder.add(SANCTIFIED_WARD_TIMER, 0);
        builder.add(IS_IN_TRANSITION, false);
        builder.add(TRANSITION_TIMER, 0);
        builder.add(LIGHT_DEVOURER_ACTIVE, false);
    }
    
    @Override
    protected void initGoals() {
        // Attack-related goals - use custom attack goal
        this.goalSelector.add(1, new KingHajileGoal(this, 1.2, false)); // Custom attack logic with phases
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ShadowQuinnEntity.class, true)); // Target Shadow Quinn
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, NickySummonerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, JoeRebelEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MillyKnightEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, ShadowQuinnEntity.class, 8.0F)); // Look at Shadow Quinn
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.KingHajile.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.KingHajile.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.KingHajile.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.KingHajile.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.KingHajile.ARMOR)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, MobConfig.KingHajile.KNOCKBACK_RESISTANCE)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.KingHajile.SCALE);
    }


    /**
     * Entity tick logic (runs every tick).
     */
    @Override
    public void tick() {
        super.tick(); // Call parent tick logic
        
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
        } else {
            // Server-side behavior
            ServerWorld world = (ServerWorld) this.getWorld();
            
            // Update attack animation timer
            int timer = this.dataTracker.get(ATTACK_ANIMATION_TIMER);
            if (timer > 0) {
                this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer - 1);
                if (timer - 1 <= 0) {
                    this.dataTracker.set(ATTACK_STATE, "");
                }
            }
            
            // Update Sanctified Ward timer
            if (this.dataTracker.get(SANCTIFIED_WARD_ACTIVE)) {
                int wardTimer = this.dataTracker.get(SANCTIFIED_WARD_TIMER);
                if (wardTimer > 0) {
                    this.dataTracker.set(SANCTIFIED_WARD_TIMER, wardTimer - 1);
                    if (wardTimer - 1 <= 0) {
                        this.dataTracker.set(SANCTIFIED_WARD_ACTIVE, false);
                    }
                } else {
                    this.dataTracker.set(SANCTIFIED_WARD_ACTIVE, false);
                }
            }
            
            // Handle phase transition
            if (this.dataTracker.get(IS_IN_TRANSITION)) {
                int transitionTimer = this.dataTracker.get(TRANSITION_TIMER);
                if (transitionTimer > 0) {
                    this.dataTracker.set(TRANSITION_TIMER, transitionTimer - 1);
                    
                    // Keep Hajile in the air during transition
                    if (this.transitionAirHeight > 0) {
                        this.setPosition(this.getX(), this.transitionAirHeight, this.getZ());
                        this.setVelocity(0, 0, 0); // Prevent any downward velocity
                    }
                    
                    // Apply totem effects 3 times during transition
                    if (totemEffectCount < 3 && transitionTimer % 100 == 0) { // Every 5 seconds (100 ticks)
                        this.playTotemEffect(world);
                        totemEffectCount++;
                    }
                    
                    // Spawn thunderstorms
                    if (transitionTimer % 20 == 0) {
                        world.setWeather(0, 600, true, true); // Force thunderstorm
                    }
                    
                    if (transitionTimer - 1 <= 0) {
                        this.completePhaseTransition();
                    }
                }
            }
            
            // Phase 2 lightning strikes (increase in intensity as health decreases)
            if (this.dataTracker.get(PHASE) == PHASE_2 && !this.dataTracker.get(IS_IN_TRANSITION)) {
                this.lightningStrikeCooldown = Math.max(this.lightningStrikeCooldown - 1, 0);
                
                if (this.lightningStrikeCooldown <= 0 && this.getTarget() != null) {
                    // Calculate intensity based on health (lower health = more frequent/intense)
                    double healthPercent = this.getHealth() / this.getMaxHealth();
                    double intensity = 1.0 - healthPercent; // 0.0 at full HP, 1.0 at 0 HP
                    
                    // Base cooldown decreases as health goes down (200 ticks at full, 50 ticks at low)
                    int baseCooldown = 200 - (int)(intensity * 150);
                    
                    // Random chance to strike (increases with lower health)
                    if (this.getRandom().nextDouble() < (0.1 + intensity * 0.4)) { // 10% at full, 50% at low
                        this.performLightningStrike(world, intensity);
                        this.lightningStrikeCooldown = baseCooldown + this.getRandom().nextInt(50);
                    }
                }
            }
            
            // Check for phase transition trigger (at 0% HP in Phase 1)
            if (this.dataTracker.get(PHASE) == PHASE_1 && this.getHealth() <= 0 && !this.dataTracker.get(IS_IN_TRANSITION)) {
                this.startPhaseTransition();
            }
            
            // Prevent movement during most attack animations
            String state = this.dataTracker.get(ATTACK_STATE);
            if (!state.isEmpty() && timer > 0) {
                if (!"ascendant_lunge".equals(state) && !"light_devourer".equals(state)) {
                    this.getNavigation().stop();
                }
            }
        }
    }
    
    private void startPhaseTransition() {
        if (this.getWorld().isClient()) return;
        
        ServerWorld world = (ServerWorld) this.getWorld();
        this.dataTracker.set(IS_IN_TRANSITION, true);
        this.dataTracker.set(TRANSITION_TIMER, 300); // 15 seconds transition
        this.totemEffectCount = 0;
        
        // Play transition dialogue
        if (this.getTarget() != null) {
            this.sendMessage(Text.literal("You reject my mercy... then face my wrath!"));
        }
        
        // Spawn particles - gold to white to corrupted blue
        for (int i = 0; i < 50; i++) {
            world.spawnParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 2, this.getZ(), 
                    10, 1.0, 1.0, 1.0, 0.1);
            world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2, this.getZ(), 
                    10, 1.0, 1.0, 1.0, 0.1);
        }
        
        // Play sound
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                this.getSoundCategory(), 1.0F, 0.5F);
        
        // Raise in air and store height
        this.setNoGravity(true);
        this.transitionAirHeight = this.getY() + 5;
        this.setPosition(this.getX(), this.transitionAirHeight, this.getZ());
        this.setVelocity(0, 0, 0); // Ensure no velocity
    }
    
    private void playTotemEffect(ServerWorld world) {
        // Spawn totem of undying particles
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double radius = 1.5;
            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY() + 1 + Math.sin(i) * 0.5;
            double z = this.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 5, 0.2, 0.2, 0.2, 0.1);
        }
        
        // Play totem sound
        world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, 
                this.getSoundCategory(), 1.0F, 1.0F);
    }
    
    private void completePhaseTransition() {
        if (this.getWorld().isClient()) return;
        
        ServerWorld world = (ServerWorld) this.getWorld();
        this.dataTracker.set(IS_IN_TRANSITION, false);
        this.dataTracker.set(PHASE, PHASE_2);
        this.setNoGravity(false);
        
        // Update boss bar color to indicate phase 2
        this.bossBar.setColor(BossBar.Color.RED);
        this.bossBar.setName(Text.literal("Hollow King Hajile"));
        
        // Spawn phase 2 particles
        for (int i = 0; i < 100; i++) {
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2, this.getZ(), 
                    20, 2.0, 2.0, 2.0, 0.2);
        }
        
        // Restore health to full for phase 2
        this.setHealth(this.getMaxHealth());
        
        // Reset lightning cooldown
        this.lightningStrikeCooldown = 0;
    }
    
    private void performLightningStrike(ServerWorld world, double intensity) {
        if (this.getTarget() == null) return;
        
        // Strike near the target or randomly in arena
        double strikeX = this.getTarget().getX() + (this.getRandom().nextDouble() - 0.5) * 10;
        double strikeZ = this.getTarget().getZ() + (this.getRandom().nextDouble() - 0.5) * 10;
        
        // Intensity determines number of strikes and damage
        int strikeCount = 1 + (int)(intensity * 3); // 1 at full HP, up to 4 at low HP
        
        // Find ground level helper
        net.minecraft.util.math.BlockPos.Mutable pos = new net.minecraft.util.math.BlockPos.Mutable();
        
        for (int i = 0; i < strikeCount; i++) {
            double angle = (i / (double) strikeCount) * Math.PI * 2;
            double radius = i * 2.0;
            double finalStrikeX = strikeX + Math.cos(angle) * radius;
            double finalStrikeZ = strikeZ + Math.sin(angle) * radius;
            
            pos.set((int) finalStrikeX, (int) (this.getY() + 20), (int) finalStrikeZ);
            while (pos.getY() > this.getY() - 10 && world.isAir(pos)) {
                pos.move(0, -1, 0);
            }
            double finalStrikeY = pos.getY() + 1;
            
            // Spawn lightning entity
            world.getServer().execute(() -> {
                net.minecraft.entity.LightningEntity lightning = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.refreshPositionAndAngles(finalStrikeX, finalStrikeY, finalStrikeZ, 0, 0);
                    world.spawnEntity(lightning);
                }
                
                // Additional visual effects
                for (int j = 0; j < 20; j++) {
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, finalStrikeX, finalStrikeY, finalStrikeZ, 
                            10, 0.5, 0.5, 0.5, 0.2);
                    world.spawnParticles(ParticleTypes.END_ROD, finalStrikeX, finalStrikeY, finalStrikeZ, 
                            5, 0.3, 0.3, 0.3, 0.1);
                }
                
                world.playSound(null, finalStrikeX, finalStrikeY, finalStrikeZ, 
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, this.getSoundCategory(), 
                        (float)(1.0 + intensity * 0.5), 0.8F + (float)(intensity * 0.4));
            });
        }
    }
    
    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }
    
    public String getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }
    
    public void setAttackState(String state) {
        this.dataTracker.set(ATTACK_STATE, state);
        int timer = 0;
        switch (state) {
            case "radiant_judgment":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "sacred_shockwave":
                timer = (int) Math.ceil(1.5 * 20); // 1.5 seconds
                break;
            case "sanctified_ward":
                timer = (int) Math.ceil(3.0 * 20); // 3 seconds
                break;
            case "orb_of_light":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "royal_command":
                timer = (int) Math.ceil(2.0 * 20); // 2 seconds
                break;
            case "blinding_flash":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "lightfall_barrage":
                timer = (int) Math.ceil(2.5 * 20); // 2.5 seconds
                break;
            case "ascendant_lunge":
                timer = (int) Math.ceil(1.0 * 20); // 1 second
                break;
            case "judgment_chains":
                timer = (int) Math.ceil(1.5 * 20); // 1.5 seconds
                break;
            case "severed_halo":
                timer = (int) Math.ceil(2.0 * 20); // 2 seconds
                break;
            case "light_devourer":
                timer = (int) Math.ceil(5.0 * 20); // 5 seconds
                break;
            default:
                timer = 0;
        }
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, timer);
    }
    
    public boolean isSanctifiedWardActive() {
        return this.dataTracker.get(SANCTIFIED_WARD_ACTIVE);
    }
    
    public void activateSanctifiedWard(int duration) {
        this.dataTracker.set(SANCTIFIED_WARD_ACTIVE, true);
        this.dataTracker.set(SANCTIFIED_WARD_TIMER, duration);
    }
    
    public boolean isLightDevourerActive() {
        return this.dataTracker.get(LIGHT_DEVOURER_ACTIVE);
    }
    
    public void setLightDevourerActive(boolean active) {
        this.dataTracker.set(LIGHT_DEVOURER_ACTIVE, active);
    }
    
    public boolean isInTransition() {
        return this.dataTracker.get(IS_IN_TRANSITION);
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Sanctified Ward reduces damage by 70%
        if (this.dataTracker.get(SANCTIFIED_WARD_ACTIVE)) {
            amount *= 0.3f; // Only 30% damage gets through (70% reduction)
        }
        
        // Prevent damage during transition
        if (this.dataTracker.get(IS_IN_TRANSITION)) {
            return false;
        }
        
        // In Phase 1, when health reaches 0, trigger transition instead of death
        if (this.dataTracker.get(PHASE) == PHASE_1 && this.getHealth() - amount <= 0 && !this.dataTracker.get(IS_IN_TRANSITION)) {
            this.startPhaseTransition();
            return false; // Prevent death
        }
        
        return super.damage(source, amount);
    }
    
    @Override
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

    /**
     * Initialize the equipment for the Milly Knight.
     */
    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);

        // Equip weapon
        ItemStack weapon = random.nextFloat() < 0.5
                ? new ItemStack(ModItems.MOON_SWORD)
                : new ItemStack(Items.DIAMOND_SWORD);
        this.equipStack(EquipmentSlot.MAINHAND, weapon);
    }

    /**
     * Get the armor items this entity is currently wearing.
     */
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return equippedItems.subList(2, 6); // Use MobEntity's built-in method
    }

    /**
     * Get the ItemStack currently equipped in a specific slot.
     */
    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return equippedItems.get(slot.getEntitySlotId());
    }

    /**
     * Equip an ItemStack to a specific slot.
     */
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        equippedItems.set(slot.getEntitySlotId(), stack);
    }

    /**
     * Define the main arm of the entity.
     */
    @Override
    public Arm getMainArm() {
        return Arm.RIGHT; // Default to right-handed
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        this.dropItem(ModItems.MOON_SWORD);
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
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_king_hajile"));
            
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
                    tracker.grantCriterion(killAdvancement, "killed_king_hajile");
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
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Phase", this.dataTracker.get(PHASE));
        nbt.putString("AttackState", this.dataTracker.get(ATTACK_STATE));
        nbt.putInt("AttackAnimationTimer", this.dataTracker.get(ATTACK_ANIMATION_TIMER));
        nbt.putBoolean("SanctifiedWardActive", this.dataTracker.get(SANCTIFIED_WARD_ACTIVE));
        nbt.putInt("SanctifiedWardTimer", this.dataTracker.get(SANCTIFIED_WARD_TIMER));
        nbt.putBoolean("IsInTransition", this.dataTracker.get(IS_IN_TRANSITION));
        nbt.putInt("TransitionTimer", this.dataTracker.get(TRANSITION_TIMER));
        nbt.putBoolean("LightDevourerActive", this.dataTracker.get(LIGHT_DEVOURER_ACTIVE));
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(PHASE, nbt.getInt("Phase"));
        this.dataTracker.set(ATTACK_STATE, nbt.getString("AttackState"));
        this.dataTracker.set(ATTACK_ANIMATION_TIMER, nbt.getInt("AttackAnimationTimer"));
        this.dataTracker.set(SANCTIFIED_WARD_ACTIVE, nbt.getBoolean("SanctifiedWardActive"));
        this.dataTracker.set(SANCTIFIED_WARD_TIMER, nbt.getInt("SanctifiedWardTimer"));
        this.dataTracker.set(IS_IN_TRANSITION, nbt.getBoolean("IsInTransition"));
        this.dataTracker.set(TRANSITION_TIMER, nbt.getInt("TransitionTimer"));
        this.dataTracker.set(LIGHT_DEVOURER_ACTIVE, nbt.getBoolean("LightDevourerActive"));
        
        // Update boss bar based on phase
        if (this.dataTracker.get(PHASE) == PHASE_2) {
            this.bossBar.setColor(BossBar.Color.RED);
            this.bossBar.setName(Text.literal("Hollow King Hajile"));
        }
    }
}
