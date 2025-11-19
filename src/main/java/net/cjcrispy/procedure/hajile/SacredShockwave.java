package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class SacredShockwave {
    private static final double SHOCKWAVE_RANGE = 8.0;
    private static final float DAMAGE = 5.0f;
    private static final double KNOCKBACK = 0.5;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Telegraph: Staff/staff raising animation would happen here
        for (int i = 0; i < 10; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 0.5, mob.getZ(), 
                    5, 0.3, 0.3, 0.3, 0.1);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_STEP, 
                mob.getSoundCategory(), 1.0F, 0.8F);
        
        // Wait 20 ticks (1 second) before shockwave
        world.getServer().execute(() -> {
            performShockwave(world, mob, 0);
        });
    }
    
    private static void performShockwave(ServerWorld world, MobEntity mob, int ring) {
        if (ring >= 10 || !mob.isAlive()) { // Expand to 10 rings
            return;
        }
        
        double currentRadius = ring * 0.8; // Each ring is 0.8 blocks further
        
        // Spawn golden ring particles
        int particleCount = 30;
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double x = mob.getX() + Math.cos(angle) * currentRadius;
            double y = mob.getY() + 0.1;
            double z = mob.getZ() + Math.sin(angle) * currentRadius;
            
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        
        // Damage and knockback entities in this ring
        Box damageBox = new Box(
                mob.getX() - currentRadius - 1, mob.getY() - 1, mob.getZ() - currentRadius - 1,
                mob.getX() + currentRadius + 1, mob.getY() + 2, mob.getZ() + currentRadius + 1
        );
        
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, damageBox, e -> 
                e != mob && e.squaredDistanceTo(mob) >= (currentRadius - 1) * (currentRadius - 1) &&
                e.squaredDistanceTo(mob) <= (currentRadius + 1) * (currentRadius + 1))) {
            entity.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
            
            // Knockback
            Vec3d direction = entity.getPos().subtract(mob.getPos()).normalize();
            entity.addVelocity(direction.x * KNOCKBACK, 0.2, direction.z * KNOCKBACK);
            entity.velocityModified = true;
        }
        
        // Clear negative effects from Hajile (holy purification)
        if (ring == 5) { // Mid-way through shockwave
            mob.clearStatusEffects();
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, false, false));
        }
        
        // Continue next ring after 3 ticks
        world.getServer().execute(() -> {
            performShockwave(world, mob, ring + 1);
        });
    }
}

