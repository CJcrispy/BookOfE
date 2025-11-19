package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class AscendantLunge {
    private static final double LUNGE_DISTANCE = 12.0;
    private static final double LUNGE_SPEED = 2.0;
    private static final float DAMAGE = 15.0f;
    private static final int TRAIL_DURATION = 20; // 1 second
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Calculate direction to target
        Vec3d mobPos = mob.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(mobPos).normalize();
        
        // Limit lunge distance
        Vec3d lungeEnd = mobPos.add(direction.multiply(LUNGE_DISTANCE));
        
        // Charge particles
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1, mob.getZ(), 
                    5, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + 1, mob.getZ(), 
                    3, 0.2, 0.2, 0.2, 0.05);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, 
                mob.getSoundCategory(), 0.8F, 1.5F);
        
        // Wait 5 ticks before lunge
        world.getServer().execute(() -> {
            performLunge(world, mob, mobPos, lungeEnd, direction, 0);
        });
    }
    
    private static void performLunge(ServerWorld world, MobEntity mob, Vec3d startPos, Vec3d endPos, Vec3d direction, int tick) {
        if (tick >= 20 || !mob.isAlive()) { // 1 second lunge
            return;
        }
        
        double progress = tick / 20.0;
        Vec3d currentPos = startPos.add(direction.multiply(LUNGE_DISTANCE * progress));
        
        // Move mob
        mob.setPosition(currentPos);
        
        // Burning light trail
        for (int i = 0; i < 3; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, currentPos.x, currentPos.y + 0.5, currentPos.z, 
                    3, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.FIREWORK, currentPos.x, currentPos.y + 0.5, currentPos.z, 
                    2, 0.15, 0.15, 0.15, 0.03);
        }
        
        // Check for hit on target
        if (tick == 10) { // Mid-lunge hit check
            if (mob.getTarget() != null && mob.squaredDistanceTo(mob.getTarget()) <= 4.0) {
                mob.tryAttack(mob.getTarget());
                mob.getTarget().damage(world.getDamageSources().mobAttack(mob), DAMAGE);
                
                // Impact effect
                for (int i = 0; i < 20; i++) {
                    world.spawnParticles(ParticleTypes.EXPLOSION, mob.getTarget().getX(), 
                            mob.getTarget().getY() + 1, mob.getTarget().getZ(), 
                            5, 0.5, 0.5, 0.5, 0.2);
                }
                
                world.playSound(null, mob.getTarget().getBlockPos(), 
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                        mob.getSoundCategory(), 0.8F, 1.2F);
            }
        }
        
        // Continue lunge
        world.getServer().execute(() -> {
            performLunge(world, mob, startPos, endPos, direction, tick + 1);
        });
    }
}

