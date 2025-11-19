package net.cjcrispy.procedure.nicky;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class NickySummonerBeamAttack {
    private static final int BEAM_DURATION_TICKS = 40; // 2 seconds
    private static final double BEAM_DAMAGE_PER_TICK = 2.0;
    private static final double BEAM_RANGE = 20.0;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement to charge the beam
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Play charging sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 
                mob.getSoundCategory(), 1.0F, 0.8F);
        
        // Charge particles (gathering energy)
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double radius = 0.5 + (i % 3) * 0.2;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1.2;
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0, 0, 0, 0.1);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0.05);
        }
        
        // Start the beam after a short charge (10 ticks = 0.5 seconds)
        world.getServer().execute(() -> {
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                    mob.getSoundCategory(), 0.5F, 1.5F);
            
            // Fire the beam for BEAM_DURATION_TICKS
            fireBeam(world, mob, target, 0);
        });
    }
    
    private static void fireBeam(ServerWorld world, MobEntity mob, LivingEntity target, int tick) {
        if (tick >= BEAM_DURATION_TICKS || !mob.isAlive() || target == null || !target.isAlive()) {
            return;
        }
        
        // Calculate direction to target
        Vec3d mobPos = mob.getPos().add(0, mob.getEyeHeight(mob.getPose()), 0);
        Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
        Vec3d direction = targetPos.subtract(mobPos);
        double distance = direction.length();
        
        // Check if target is in range
        if (distance > BEAM_RANGE) {
            return;
        }
        
        direction = direction.normalize();
        
        // Create beam particles along the path
        int particleCount = (int) (distance * 2);
        for (int i = 0; i <= particleCount; i++) {
            double progress = i / (double) particleCount;
            Vec3d beamPos = mobPos.add(direction.multiply(distance * progress));
            
            // Main beam particles
            world.spawnParticles(ParticleTypes.END_ROD, beamPos.x, beamPos.y, beamPos.z, 
                    1, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, beamPos.x, beamPos.y, beamPos.z, 
                    1, 0.15, 0.15, 0.15, 0.03);
            
            // Glow effect
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.FIREWORK, beamPos.x, beamPos.y, beamPos.z, 
                        1, 0.2, 0.2, 0.2, 0.02);
            }
        }
        
        // Damage target every 5 ticks (but not minions)
        if (tick % 5 == 0 && distance <= BEAM_RANGE) {
            // Only damage if target is not a BlackBird (minion)
            if (!(target instanceof net.cjcrispy.entity.custom.BlackBirdEntity)) {
                target.damage(world.getDamageSources().mobAttack(mob), (float) BEAM_DAMAGE_PER_TICK);
                
                // Knockback effect
                Vec3d knockback = direction.multiply(0.1);
                target.addVelocity(knockback.x, 0.05, knockback.z);
                target.velocityModified = true;
                
                // Impact particles on target
                for (int i = 0; i < 5; i++) {
                    world.spawnParticles(ParticleTypes.EXPLOSION, targetPos.x, targetPos.y, targetPos.z, 
                            1, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
        
        // Continue beam on next tick
        world.getServer().execute(() -> {
            fireBeam(world, mob, target, tick + 1);
        });
    }
}

