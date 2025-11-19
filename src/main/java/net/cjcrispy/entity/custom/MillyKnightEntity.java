package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.cjcrispy.entity.ai.MillyKnightGoal;
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
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

public class MillyKnightEntity extends HostileEntity {

    // Store equipped armor and hand items
    private final DefaultedList<ItemStack> equippedItems = DefaultedList.ofSize(6, ItemStack.EMPTY);

    // Crossbow barrage state
    private boolean crossbowBarrageActive;
    private int crossbowBarrageShotTimer;
    private int crossbowBarrageShotInterval;
    private int crossbowBarrageShotsRemaining;
    private float crossbowBarrageProjectileVelocity;
    private LivingEntity crossbowBarrageTarget;

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Milly, Knight Commander"),
            BossBar.Color.WHITE, BossBar.Style.NOTCHED_10);

    public MillyKnightEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.initEquipment(world.getRandom(), world.getLocalDifficulty(this.getBlockPos()));
    }


    @Override
    protected void initGoals() {
        // Attack-related goals
        this.goalSelector.add(1, new MillyKnightGoal(this, 1.2, false)); // 20 ticks = 1 sec cooldown
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true)); // Target players
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true)); // Target iron golems
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, KingHajileEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, ShadowQuinnEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, NickySummonerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, JoeRebelEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, SlimeChrisEntity.class, true));
        this.targetSelector.add(4, new RevengeGoal(this)); // Revenge against the last attacker

        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0)); // Wander randomly
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F)); // Look at players
        this.goalSelector.add(4, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F)); // Look at golems
        this.goalSelector.add(5, new LookAtEntityGoal(this, ShadowQuinnEntity.class, 8.0F)); // Look at Shadow Quinn
        this.goalSelector.add(5, new LookAtEntityGoal(this, SlimeChrisEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, KingHajileEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, NickySummonerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, JoeRebelEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this)); // Look around when idle
        this.goalSelector.add(7, new SwimGoal(this)); // Swim when underwater
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.MillyKnight.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.MillyKnight.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.MillyKnight.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.MillyKnight.FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.MillyKnight.ARMOR)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, MobConfig.MillyKnight.KNOCKBACK_RESISTANCE)
                .add(EntityAttributes.GENERIC_SCALE, MobConfig.MillyKnight.SCALE);
    }


    /**
     * Entity tick logic (runs every tick).
     */
    @Override
    public void tick() {
        super.tick(); // Call parent tick logic
        // Add custom behavior here, such as effects or AI adjustments
        if (this.getWorld().isClient()) {
            // Client-side effects, like particles or animations
        } else {
            // Server-side behavior
        }
    }

    /**
     * Initialize the equipment for the Milly Knight.
     */
    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);

        // Equip weapon
        ItemStack weapon = new ItemStack(ModItems.BLACKBORN);
        this.equipStack(EquipmentSlot.MAINHAND, weapon);
        this.setStackInHand(Hand.MAIN_HAND, weapon.copy());
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
        ItemStack stored = equippedItems.get(slot.getEntitySlotId());
        return stored.isEmpty() ? super.getEquippedStack(slot) : stored;
    }

    /**
     * Equip an ItemStack to a specific slot.
     */
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        super.equipStack(slot, stack);
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

        this.dropItem(ModItems.BLACKBORN);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        // Grant advancement to all players who participated in the fight (similar to Ender Dragon/Wither)
        if (!this.getWorld().isClient() && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            // Ensure root advancement is granted first
            net.minecraft.advancement.AdvancementEntry rootAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "root"));
            net.minecraft.advancement.AdvancementEntry killAdvancement = this.getServer().getAdvancementLoader()
                    .get(net.minecraft.util.Identifier.of("bookofe", "custom/kill_milly_knight"));
            
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
                    tracker.grantCriterion(killAdvancement, "killed_milly_knight");
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
        if (!this.getWorld().isClient()) {
            this.handleCrossbowBarrage();
        }
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

    /* CROSSBOW BARRAGE CONTROL */

    public boolean isCrossbowBarrageActive() {
        return crossbowBarrageActive;
    }

    public void startCrossbowBarrage(LivingEntity target, int totalShots, int intervalTicks, float projectileVelocity) {
        if (this.getWorld().isClient() || target == null || !target.isAlive()) return;

        this.crossbowBarrageActive = true;
        this.crossbowBarrageTarget = target;
        this.crossbowBarrageShotsRemaining = Math.max(1, totalShots);
        this.crossbowBarrageShotInterval = Math.max(2, intervalTicks);
        this.crossbowBarrageShotTimer = 5; // slight wind-up before the first shot
        this.crossbowBarrageProjectileVelocity = projectileVelocity;

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        this.equipStack(EquipmentSlot.MAINHAND, crossbow);
        this.setStackInHand(Hand.MAIN_HAND, crossbow.copy());

        int slowDuration = this.crossbowBarrageShotsRemaining * this.crossbowBarrageShotInterval + 20;
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, 1, false, false));
    }

    private void handleCrossbowBarrage() {
        if (!crossbowBarrageActive) return;

        if (crossbowBarrageTarget == null || !crossbowBarrageTarget.isAlive()) {
            endCrossbowBarrage();
            return;
        }

        this.getLookControl().lookAt(crossbowBarrageTarget, 30.0F, 30.0F);

        if (crossbowBarrageShotTimer > 0) {
            crossbowBarrageShotTimer--;
            return;
        }

        if (crossbowBarrageShotsRemaining <= 0) {
            endCrossbowBarrage();
            return;
        }

        fireCrossbowArrow(crossbowBarrageTarget);
        crossbowBarrageShotsRemaining--;
        crossbowBarrageShotTimer = crossbowBarrageShotInterval;

        if (crossbowBarrageShotsRemaining <= 0) {
            endCrossbowBarrage();
        }
    }

    private void fireCrossbowArrow(LivingEntity target) {
        if (this.getWorld().isClient()) return;

        double targetX = target.getX() - this.getX();
        double targetY = target.getBodyY(0.33D) - this.getBodyY(0.33D);
        double targetZ = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);

        ArrowEntity arrowEntity = EntityType.ARROW.create(this.getWorld());
        if (arrowEntity == null) return;
        arrowEntity.refreshPositionAndAngles(this.getX(), this.getBodyY(0.5D), this.getZ(), this.getYaw(), this.getPitch());
        arrowEntity.setOwner(this);
        arrowEntity.setVelocity(targetX, targetY + horizontalDistance * 0.2D, targetZ, this.crossbowBarrageProjectileVelocity, 0.8F);
        arrowEntity.setDamage(8.0D);
        arrowEntity.setCritical(true);
        arrowEntity.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;

        this.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0F, 0.9F + this.getRandom().nextFloat() * 0.2F);
        this.getWorld().spawnEntity(arrowEntity);
    }

    private void endCrossbowBarrage() {
        this.crossbowBarrageActive = false;
        this.crossbowBarrageTarget = null;
        this.crossbowBarrageShotsRemaining = 0;
        this.crossbowBarrageShotTimer = 0;
        this.crossbowBarrageProjectileVelocity = 0.0F;

        this.ensureBlackbornEquipped();
    }

    public void ensureBlackbornEquipped() {
        if (this.isHolding(stack -> stack.isOf(ModItems.BLACKBORN))) return;

        ItemStack weapon = new ItemStack(ModItems.BLACKBORN);
        weapon.setCount(1);
        this.equipStack(EquipmentSlot.MAINHAND, weapon);
        this.setStackInHand(Hand.MAIN_HAND, weapon.copy());
    }
}
