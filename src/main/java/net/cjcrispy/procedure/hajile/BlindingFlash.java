package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

public class BlindingFlash {
    private static final double FLASH_RANGE = 50.0;
    private static final float DAMAGE = 2.0f;
    private static final int BURN_DURATION = 40; // 2 seconds
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        
        // Build-up particles
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 1.0 + (i % 3) * 0.3;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1 + Math.sin(i) * 0.5;
            double z = mob.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 5, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 3, 0.15, 0.15, 0.15, 0.05);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                mob.getSoundCategory(), 0.5F, 2.0F);
        
        // Wait 20 ticks (1 second) before flash
        world.getServer().execute(() -> {
            performFlash(world, mob, 0);
        });
    }
    
    private static void performFlash(ServerWorld world, MobEntity mob, int tick) {
        if (tick >= 40 || !mob.isAlive()) { // 2 seconds duration
            return;
        }
        
        // Massive flash at start
        if (tick == 0) {
            // Blind all players in range
            Box flashBox = new Box(
                    mob.getX() - FLASH_RANGE, mob.getY() - 10, mob.getZ() - FLASH_RANGE,
                    mob.getX() + FLASH_RANGE, mob.getY() + 10, mob.getZ() + FLASH_RANGE
            );
            
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, flashBox, 
                    p -> p.squaredDistanceTo(mob) <= FLASH_RANGE * FLASH_RANGE)) {
                // Apply blindness
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0, false, false));
                
                // Light burn damage
                player.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
                
                // Apply radiant burn (glowing + slight damage over time)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, BURN_DURATION, 0, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 0, false, false));
            }
            
            // Massive particle explosion
            for (int i = 0; i < 200; i++) {
                double angle = (i / 200.0) * Math.PI * 2;
                double radius = FLASH_RANGE * (0.5 + mob.getRandom().nextDouble() * 0.5);
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY() + 1 + (mob.getRandom().nextDouble() - 0.5) * 5;
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 3, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.15, 0.15, 0.15, 0.05);
            }
            
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                    mob.getSoundCategory(), 1.0F, 0.5F);
        }
        
        // Leave glowing cracks on floor
        if (tick % 5 == 0 && tick < 20) {
            for (int i = 0; i < 10; i++) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = mob.getRandom().nextDouble() * FLASH_RANGE * 0.5;
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY() - 0.5;
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 5, 0.5, 0.1, 0.5, 0.05);
            }
        }
        
        // Continue flash effect
        world.getServer().execute(() -> {
            performFlash(world, mob, tick + 1);
        });
    }
}

