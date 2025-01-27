package net.cjcrispy.entity.custom;

import net.cjcrispy.item.ModItems;
import net.minecraft.enchantment.Enchantments;
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
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

public class MillyKnightEntity extends HostileEntity {

    // Store equipped armor and hand items
    private final DefaultedList<ItemStack> equippedItems = DefaultedList.ofSize(6, ItemStack.EMPTY);

    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Milly, Knight Commander"),
            BossBar.Color.RED, BossBar.Style.NOTCHED_10);

    public MillyKnightEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 250)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
                .add(EntityAttributes.GENERIC_ARMOR, 8)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 6)
                .add(EntityAttributes.GENERIC_SCALE, 1.35);
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

        // Randomly select a weapon
        ItemStack weapon;
        if (random.nextFloat() < 0.5) {
            weapon = new ItemStack(ModItems.BLACKBORN);
        } else {
            weapon = new ItemStack(Items.DIAMOND_SWORD);
        }

        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));

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
        if (slot == EquipmentSlot.MAINHAND) {
            // Access the main hand stack directly
            return this.equippedItems.get(EquipmentSlot.MAINHAND.getEntitySlotId());
        } else {
            return super.getEquippedStack(slot); // Handle other slots
        }
    }

    /**
     * Equip an ItemStack to a specific slot.
     */
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            // Set the main hand stack directly
            this.equippedItems.set(EquipmentSlot.MAINHAND.getEntitySlotId(), stack);
        } else {
            super.equipStack(slot, stack); // Handle other slots
        }
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
}
