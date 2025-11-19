package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

public class OrbOfLight {
    private static final int ORB_LIFETIME = 300; // 15 seconds
    private static final double ORB_SPEED = 0.05;
    private static final double ORB_ORBIT_RADIUS = 12.0;
    private static final float TOUCH_DAMAGE = 3.0f;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Create 1-2 orbs
        int orbCount = mob.getRandom().nextInt(2) + 1;
        
        for (int i = 0; i < orbCount; i++) {
            createOrb(world, mob, i, orbCount);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
                mob.getSoundCategory(), 0.8F, 1.2F);
    }
    
    private static void createOrb(ServerWorld world, MobEntity mob, int index, int total) {
        double startAngle = (index / (double) total) * Math.PI * 2;
        
        // Spawn orb entity or track as moving particle effect
        world.getServer().execute(() -> {
            orbitOrb(world, mob, startAngle, 0);
        });
    }
    
    private static void orbitOrb(ServerWorld world, MobEntity mob, double angle, int age) {
        if (age >= ORB_LIFETIME || !mob.isAlive()) {
            // Orb expires - final burst
            for (int i = 0; i < 10; i++) {
                double x = mob.getX() + Math.cos(angle) * ORB_ORBIT_RADIUS;
                double y = mob.getY() + 2;
                double z = mob.getZ() + Math.sin(angle) * ORB_ORBIT_RADIUS;
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 5, 0.3, 0.3, 0.3, 0.1);
            }
            return;
        }
        
        // Calculate orb position (orbiting around Hajile)
        double currentAngle = angle + (age * ORB_SPEED);
        double x = mob.getX() + Math.cos(currentAngle) * ORB_ORBIT_RADIUS;
        double y = mob.getY() + 2 + Math.sin(age * 0.1) * 0.5; // Bob up and down
        double z = mob.getZ() + Math.sin(currentAngle) * ORB_ORBIT_RADIUS;
        
        // Orb particles (glowing orb)
        for (int i = 0; i < 5; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0.15, 0.15, 0.15, 0.03);
        }
        
        // Illuminate area (light level increase - visual effect)
        if (age % 20 == 0) {
            world.spawnParticles(ParticleTypes.GLOW, x, y, z, 10, 1.0, 1.0, 1.0, 0.05);
        }
        
        // Check for player contact
        net.minecraft.util.math.Box orbBox = new net.minecraft.util.math.Box(
                x - 1, y - 1, z - 1,
                x + 1, y + 1, z + 1
        );
        
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, orbBox, 
                e -> e != mob && e instanceof net.minecraft.entity.player.PlayerEntity)) {
            entity.damage(world.getDamageSources().mobAttack(mob), TOUCH_DAMAGE);
            
            // Light damage particles
            for (int i = 0; i < 5; i++) {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1, entity.getZ(), 
                        3, 0.3, 0.3, 0.3, 0.1);
            }
            
            world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 
                    mob.getSoundCategory(), 0.5F, 1.5F);
        }
        
        // Continue orbit on next tick
        world.getServer().execute(() -> {
            orbitOrb(world, mob, angle, age + 1);
        });
    }
}

