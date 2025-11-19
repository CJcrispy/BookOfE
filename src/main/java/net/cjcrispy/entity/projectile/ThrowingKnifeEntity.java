package net.cjcrispy.entity.projectile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ThrowingKnifeEntity extends ArrowEntity {
    private boolean hasRicocheted = false;
    
    public ThrowingKnifeEntity(EntityType<? extends ArrowEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public ThrowingKnifeEntity(World world, LivingEntity owner, double x, double y, double z) {
        super(EntityType.ARROW, world);
        this.setOwner(owner);
        this.refreshPositionAndAngles(x, y, z, this.getYaw(), this.getPitch());
        this.pickupType = PickupPermission.DISALLOWED;
        this.setDamage(4.0); // Light damage
        this.setCritical(false);
    }
    
    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(Items.IRON_SWORD);
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld().isClient()) return;
        
        LivingEntity owner = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;
        if (entityHitResult.getEntity() instanceof LivingEntity target && target != owner) {
            // Apply damage
            DamageSource damageSource = this.getDamageSources().arrow(this, owner);
            target.damage(damageSource, (float) this.getDamage());
            
            // Apply slowness for 1 second (20 ticks)
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0, false, false));
            
            // Spawn hit particles
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            }
            
            // Remove the knife after hitting
            this.discard();
        }
    }
    
    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld().isClient()) return;
        
        // Ricochet once if not already ricocheted
        if (!hasRicocheted) {
            hasRicocheted = true;
            Vec3d velocity = this.getVelocity();
            Vec3d normal = blockHitResult.getPos().subtract(this.getPos()).normalize();
            
            // Reflect the velocity
            Vec3d reflected = velocity.subtract(normal.multiply(2 * velocity.dotProduct(normal)));
            this.setVelocity(reflected.multiply(0.7)); // Reduce speed slightly
            
            // Spawn ricochet particles
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
            }
        } else {
            // Remove after ricochet
            this.discard();
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Remove if it's been in the air too long (5 seconds)
        if (this.age > 100) {
            this.discard();
        }
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("HasRicocheted", hasRicocheted);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        hasRicocheted = nbt.getBoolean("HasRicocheted");
    }
}

