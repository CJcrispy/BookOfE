package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class JoeRebelCloneEntity extends HostileEntity {
    private int lifetime = 200; // 10 seconds lifetime
    
    public JoeRebelCloneEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        // Scale is set via attributes (1.0 in createAttributes)
    }
    
    @Override
    protected void initGoals() {
        // Attack-related goals
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.add(3, new RevengeGoal(this));
        
        // General AI behavior goals
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new SwimGoal(this));
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.JoeRebel.MAX_HEALTH * 0.3) // 30% of Joe's health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.JoeRebel.MOVEMENT_SPEED * 1.1) // Slightly faster
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.JoeRebel.ATTACK_DAMAGE * 0.5) // 50% of Joe's damage
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.JoeRebel.FOLLOW_RANGE * 0.5) // Shorter range
                .add(EntityAttributes.GENERIC_ARMOR, MobConfig.JoeRebel.ARMOR * 0.5) // Less armor
                .add(EntityAttributes.GENERIC_SCALE, 1.0); // Scale 1
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient()) {
            // Decrease lifetime
            lifetime--;
            if (lifetime <= 0) {
                // Despawn with smoke particles
                if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    for (int i = 0; i < 10; i++) {
                        serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, 
                            this.getX(), this.getY() + 0.5, this.getZ(), 
                            3, 0.3, 0.3, 0.3, 0.05);
                    }
                }
                this.discard();
            }
        }
    }
    
    @Override
    protected void initEquipment(net.minecraft.util.math.random.Random random, net.minecraft.world.LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);
        // Equip iron sword
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        this.equipStack(EquipmentSlot.MAINHAND, sword);
        this.setStackInHand(Hand.MAIN_HAND, sword);
    }
    
    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        // Clones don't drop loot
    }
    
    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }
}

