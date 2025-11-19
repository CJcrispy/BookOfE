package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.SlimeChrisGoal;
import net.cjcrispy.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

public class SlimeChrisEntity extends HostileEntity {

    // Store equipped armor and hand items
    private final DefaultedList<ItemStack> equippedItems = DefaultedList.ofSize(6, ItemStack.EMPTY);

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Chris, Slime Cultist"),
            BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);

    // Gelatinous Shield state
    private boolean gelatinousShieldActive = false;
    private int gelatinousShieldCooldown = 0;
    private static final int GELATINOUS_SHIELD_DURATION = 200; // 10 seconds

    // Corrupted Strength state
    private boolean corruptedStrengthActive = false;
    private int corruptedStrengthTimer = 0;
    private static final int CORRUPTED_STRENGTH_DURATION = 200; // 10 seconds
    private boolean hasTriggeredCorruptedStrength = false;

    // Track attackers for summoned slimes
    private final java.util.Set<java.util.UUID> attackers = new java.util.HashSet<>();

    public SlimeChrisEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.initEquipment(world.getRandom(), world.getLocalDifficulty(this.getBlockPos()));
    }

    @Override
    protected void initGoals() {
        // Attack-related goals - use custom goal with movesets
        this.goalSelector.add(1, new SlimeChrisGoal(this, 1.2, false));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, KingHajileEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ShadowQuinnEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, NickySummonerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, JoeRebelEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MillyKnightEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, ShadowQuinnEntity.class, 8.0F)); // Look at Shadow Quinn
        this.goalSelector.add(5, new LookAtEntityGoal(this, MillyKnightEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, KingHajileEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, NickySummonerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, JoeRebelEntity.class, 8.0F));

        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.SlimeChris.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.SlimeChris.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.SlimeChris.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.SlimeChris.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.SlimeChris.ARMOR)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, MobConfig.SlimeChris.KNOCKBACK_RESISTANCE)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.SlimeChris.SCALE);
    }


    /**
     * Entity tick logic (runs every tick).
     */
    @Override
    public void tick() {
        super.tick(); // Call parent tick logic
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
            if (gelatinousShieldActive) {
                // Glow effect when shield is active
                this.setGlowing(true);
            } else {
                this.setGlowing(false);
            }
        } else {
            // Server-side behavior
            handleGelatinousShield();
            handleCorruptedStrength();
            handleHarmfulSlimePuddles();
        }
    }

    /**
     * Initialize the equipment for the Slime Chris.
     */
    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);

        // Equip weapon
        ItemStack weapon = random.nextFloat() < 0.5
                ? new ItemStack(ModItems.SLIME_HAMMER)
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

        this.dropItem(ModItems.SLIME_HAMMER);
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
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_slime_chris"));
            
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
                    tracker.grantCriterion(killAdvancement, "killed_slime_chris");
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

    /**
     * Override damage to handle Gelatinous Shield and track attackers
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) {
            return super.damage(source, amount);
        }

        // Track attackers for summoned slimes
        if (source.getAttacker() instanceof LivingEntity attacker) {
            attackers.add(attacker.getUuid());
            // Update all summoned slimes to target this attacker
            updateSummonedSlimesTarget(attacker);
        }

        // Check if Gelatinous Shield is active
        if (gelatinousShieldActive) {
            // Reduce damage by 70%
            float reducedAmount = amount * 0.3f;
            
            // Play rubbery sound when hit
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_BREAK,
                        this.getSoundCategory(), 1.5F, 0.8F);
            }

            // Reflect 10% of melee damage as knockback
            if (source.getAttacker() instanceof LivingEntity attacker && 
                source.getSource() instanceof LivingEntity && 
                attacker.squaredDistanceTo(this) < 16.0) {
                Vec3d knockback = new Vec3d(
                        attacker.getX() - this.getX(),
                        0.0,
                        attacker.getZ() - this.getZ()
                ).normalize().multiply(amount * 0.1);
                attacker.addVelocity(knockback.x, 0.2, knockback.z);
                attacker.velocityModified = true;
            }

            return super.damage(source, reducedAmount);
        }

        return super.damage(source, amount);
    }

    /**
     * Handle Gelatinous Shield activation and cooldown
     */
    private void handleGelatinousShield() {
        if (gelatinousShieldActive) {
            gelatinousShieldCooldown--;
            if (gelatinousShieldCooldown <= 0) {
                gelatinousShieldActive = false;
                this.setGlowing(false);
            }
        } else {
            // Activate shield randomly or when health is low
            if (this.getHealth() < this.getMaxHealth() * 0.5 && 
                this.getRandom().nextInt(100) < 5) { // 5% chance per tick when below 50% HP
                activateGelatinousShield();
            }
        }
    }

    /**
     * Activate Gelatinous Shield
     */
    public void activateGelatinousShield() {
        if (gelatinousShieldActive) return;
        
        gelatinousShieldActive = true;
        gelatinousShieldCooldown = GELATINOUS_SHIELD_DURATION;
        this.setGlowing(true);

        // Visual effect
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 20; i++) {
                serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME,
                        this.getX(), this.getY() + 1, this.getZ(),
                        5, 0.5, 0.5, 0.5, 0.1);
            }
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE,
                    this.getSoundCategory(), 1.5F, 0.6F);
        }
    }

    /**
     * Handle Corrupted Strength phase (activates at 25% HP)
     */
    private void handleCorruptedStrength() {
        // Check if we should trigger corrupted strength
        if (!hasTriggeredCorruptedStrength && 
            this.getHealth() <= this.getMaxHealth() * 0.25) {
            triggerCorruptedStrength();
        }

        // Handle active corrupted strength
        if (corruptedStrengthActive) {
            corruptedStrengthTimer--;
            if (corruptedStrengthTimer <= 0) {
                corruptedStrengthActive = false;
                this.removeStatusEffect(StatusEffects.STRENGTH);
            } else {
                // Reapply strength effect
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, false, false));
            }
        }
    }

    /**
     * Trigger Corrupted Strength
     */
    private void triggerCorruptedStrength() {
        hasTriggeredCorruptedStrength = true;
        corruptedStrengthActive = true;
        corruptedStrengthTimer = CORRUPTED_STRENGTH_DURATION;

        // Apply strength effect
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, CORRUPTED_STRENGTH_DURATION, 0, false, false));

        // Visual and sound effects
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 30; i++) {
                serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME,
                        this.getX(), this.getY() + 1, this.getZ(),
                        10, 1.0, 1.0, 1.0, 0.2);
            }
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH,
                    this.getSoundCategory(), 2.0F, 0.3F);
        }
    }

    /**
     * Make slime puddles harmful when corrupted strength is active
     */
    private void handleHarmfulSlimePuddles() {
        if (!corruptedStrengthActive) return;

        // Find nearby slime entities (puddles)
        Box searchBox = this.getBoundingBox().expand(10.0);
        for (SlimeEntity slime : this.getWorld().getEntitiesByClass(
                SlimeEntity.class, searchBox, 
                slime -> slime.isAlive() && this.squaredDistanceTo(slime) <= 100.0)) {
            
            // Find entities standing on or near the slime
            Box slimeBox = slime.getBoundingBox().expand(0.5, 0.1, 0.5);
            for (LivingEntity entity : this.getWorld().getEntitiesByClass(
                    LivingEntity.class, slimeBox,
                    e -> e != this && e.isAlive() && e instanceof PlayerEntity)) {
                
                // Apply poison and wither effects
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 0, false, true));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, true));
            }
        }
    }

    /**
     * Ensure slime hammer is equipped
     */
    public void ensureSlimeHammerEquipped() {
        if (this.isHolding(stack -> stack.isOf(ModItems.SLIME_HAMMER))) return;

        ItemStack weapon = new ItemStack(ModItems.SLIME_HAMMER);
        weapon.setCount(1);
        this.equipStack(EquipmentSlot.MAINHAND, weapon);
        this.setStackInHand(Hand.MAIN_HAND, weapon.copy());
    }

    public boolean isGelatinousShieldActive() {
        return gelatinousShieldActive;
    }

    public boolean isCorruptedStrengthActive() {
        return corruptedStrengthActive;
    }

    /**
     * Get the set of attacker UUIDs
     */
    public java.util.Set<java.util.UUID> getAttackers() {
        return java.util.Collections.unmodifiableSet(attackers);
    }

    /**
     * Update all summoned slimes to target a new attacker
     */
    private void updateSummonedSlimesTarget(LivingEntity attacker) {
        if (this.getWorld().isClient()) return;

        Box searchBox = this.getBoundingBox().expand(50.0);
        for (net.cjcrispy.entity.custom.SlimeCommonEntity slime : this.getWorld().getEntitiesByClass(
                net.cjcrispy.entity.custom.SlimeCommonEntity.class, searchBox,
                slime -> slime.isAlive() && 
                         slime.getOwnerUuid() != null && 
                         slime.getOwnerUuid().equals(this.getUuid()))) {
            // Set the attacker as target if the slime doesn't have a target or the attacker is closer
            if (slime.getTarget() == null || 
                (attacker.isAlive() && slime.squaredDistanceTo(attacker) < slime.squaredDistanceTo(slime.getTarget()))) {
                slime.setTarget(attacker);
            }
        }
    }
}
