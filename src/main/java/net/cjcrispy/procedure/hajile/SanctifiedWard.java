package net.cjcrispy.procedure.hajile;

import net.cjcrispy.entity.custom.KingHajileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

public class SanctifiedWard {
    private static final int WARD_DURATION = 60; // 3 seconds
    
    public static void execute(Entity entity) {
        if (!(entity instanceof KingHajileEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        
        // Activate ward
        mob.activateSanctifiedWard(WARD_DURATION);
        
        // Visual effect: Golden shield particles
        for (int i = 0; i < 50; i++) {
            double angle = (i / 50.0) * Math.PI * 2;
            double radius = 1.5;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1 + Math.sin(i * 0.5) * 0.5;
            double z = mob.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 3, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 2, 0.08, 0.08, 0.08, 0.02);
        }
        
        // Play choir-like hum sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_BELL_USE, 
                mob.getSoundCategory(), 1.0F, 0.5F);
        world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
                mob.getSoundCategory(), 0.8F, 0.8F);
        
        // Create shield that deflects projectiles
        world.getServer().execute(() -> {
            maintainWard(world, mob, 0);
        });
    }
    
    private static void maintainWard(ServerWorld world, KingHajileEntity mob, int tick) {
        if (tick >= WARD_DURATION || !mob.isAlive() || !mob.isSanctifiedWardActive()) {
            return;
        }
        
        // Continuous shield particles
        if (tick % 5 == 0) {
            for (int i = 0; i < 10; i++) {
                double angle = (i / 10.0) * Math.PI * 2 + (tick * 0.1);
                double radius = 1.5;
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY() + 1;
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        
        // Deflect projectiles (push them back)
        net.minecraft.util.math.Box shieldBox = mob.getBoundingBox().expand(2.0);
        for (net.minecraft.entity.projectile.ProjectileEntity projectile : 
                world.getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class, shieldBox, 
                        p -> p != null && p.getOwner() != mob)) {
            net.minecraft.util.math.Vec3d direction = projectile.getPos().subtract(mob.getPos()).normalize();
            projectile.setVelocity(direction.multiply(0.5)); // Push back at 50% speed
            projectile.velocityModified = true;
            
            // Spark effect
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(), 
                    5, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Continue ward on next tick
        world.getServer().execute(() -> {
            maintainWard(world, mob, tick + 1);
        });
    }
}

