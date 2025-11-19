package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

public class JudgmentChains {
    private static final int CHAIN_COUNT = 3;
    private static final double CHAIN_RANGE = 12.0;
    private static final float DAMAGE = 2.0f;
    private static final int CHAIN_DURATION = 200; // 10 seconds
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        
        // Cast particles
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1, mob.getZ(), 
                    5, 0.3, 0.3, 0.3, 0.1);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, 
                mob.getSoundCategory(), 1.0F, 0.8F);
        
        // Wait 20 ticks before chains
        world.getServer().execute(() -> {
            spawnChains(world, mob, target, 0);
        });
    }
    
    private static void spawnChains(ServerWorld world, MobEntity mob, LivingEntity target, int chainIndex) {
        if (chainIndex >= CHAIN_COUNT || !mob.isAlive() || target == null || !target.isAlive()) {
            return;
        }
        
        // Spawn chain around target
        double angle = (chainIndex / (double) CHAIN_COUNT) * Math.PI * 2;
        double radius = 2.0;
        double chainX = target.getX() + Math.cos(angle) * radius;
        double chainY = target.getY();
        double chainZ = target.getZ() + Math.sin(angle) * radius;
        
        // Create chain effect
        for (int i = 0; i < 10; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, chainX, chainY + i * 0.2, chainZ, 
                    3, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticles(ParticleTypes.FIREWORK, chainX, chainY + i * 0.2, chainZ, 
                    2, 0.15, 0.15, 0.15, 0.05);
        }
        
        world.playSound(null, chainX, chainY, chainZ, SoundEvents.BLOCK_CHAIN_PLACE, 
                mob.getSoundCategory(), 0.8F, 1.2F);
        
        // Start chain effect on target
        world.getServer().execute(() -> {
            applyChainEffect(world, mob, target, chainX, chainY, chainZ, 0);
        });
        
        // Spawn next chain after delay
        world.getServer().execute(() -> {
            spawnChains(world, mob, target, chainIndex + 1);
        });
    }
    
    private static void applyChainEffect(ServerWorld world, MobEntity mob, LivingEntity target, 
                                         double chainX, double chainY, double chainZ, int age) {
        if (age >= CHAIN_DURATION || !mob.isAlive() || target == null || !target.isAlive()) {
            return;
        }
        
        // Check if chain is broken (target moved far away)
        double distance = target.squaredDistanceTo(chainX, chainY, chainZ);
        if (distance > CHAIN_RANGE * CHAIN_RANGE) {
            // Chain broken effect
            for (int i = 0; i < 10; i++) {
                world.spawnParticles(ParticleTypes.EXPLOSION, chainX, chainY, chainZ, 
                        3, 0.3, 0.3, 0.3, 0.1);
            }
            world.playSound(null, chainX, chainY, chainZ, SoundEvents.BLOCK_CHAIN_BREAK, 
                    mob.getSoundCategory(), 0.8F, 1.5F);
            return;
        }
        
        // Apply slow and chip damage
        if (age % 20 == 0) { // Every second
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false));
            target.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
            
            // Chain particles connecting to target
            net.minecraft.util.math.Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
            net.minecraft.util.math.Vec3d chainPos = new net.minecraft.util.math.Vec3d(chainX, chainY + 1, chainZ);
            net.minecraft.util.math.Vec3d direction = targetPos.subtract(chainPos).normalize();
            double distanceToTarget = chainPos.distanceTo(targetPos);
            
            int particleCount = (int) (distanceToTarget * 5);
            for (int i = 0; i <= particleCount; i++) {
                double progress = i / (double) particleCount;
                net.minecraft.util.math.Vec3d linkPos = chainPos.add(direction.multiply(distanceToTarget * progress));
                
                world.spawnParticles(ParticleTypes.END_ROD, linkPos.x, linkPos.y, linkPos.z, 
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        
        // Visible chain particles
        if (age % 5 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, chainX, chainY, chainZ, 
                    2, 0.2, 0.2, 0.2, 0.05);
        }
        
        // Continue chain effect
        world.getServer().execute(() -> {
            applyChainEffect(world, mob, target, chainX, chainY, chainZ, age + 1);
        });
    }
}

