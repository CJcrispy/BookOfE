package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class RadiantJudgment {
    private static final double BEAM_RANGE = 30.0;
    private static final float DAMAGE = 8.0f;
    private static final float UNDEAD_DAMAGE_MULTIPLIER = 2.0f;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement to aim
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        mob.getLookControl().lookAt(target, 30.0F, 30.0F);
        
        // Telegraph: Golden particles gather in palm
        Vec3d handPos = mob.getPos().add(0, mob.getEyeHeight(mob.getPose()), 0)
                .add(mob.getRotationVector().multiply(0.5));
        
        for (int i = 0; i < 15; i++) {
            world.spawnParticles(ParticleTypes.FIREWORK, handPos.x, handPos.y, handPos.z, 
                    3, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, handPos.x, handPos.y, handPos.z, 
                    2, 0.15, 0.15, 0.15, 0.03);
        }
        
        // Wait 10 ticks (0.5 seconds) before firing
        world.getServer().execute(() -> {
            fireBeam(world, mob, target, handPos, 0);
        });
    }
    
    private static void fireBeam(ServerWorld world, MobEntity mob, LivingEntity target, Vec3d startPos, int tick) {
        if (tick >= 20 || !mob.isAlive() || target == null || !target.isAlive()) { // 1 second beam
            return;
        }
        
        Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
        Vec3d direction = targetPos.subtract(startPos);
        double distance = direction.length();
        
        if (distance > BEAM_RANGE) {
            return;
        }
        
        direction = direction.normalize();
        
        // Create glowing line of particles
        int particleCount = (int) (distance * 3);
        for (int i = 0; i <= particleCount; i++) {
            double progress = i / (double) particleCount;
            Vec3d beamPos = startPos.add(direction.multiply(distance * progress));
            
            world.spawnParticles(ParticleTypes.END_ROD, beamPos.x, beamPos.y, beamPos.z, 
                    2, 0.05, 0.05, 0.05, 0.02);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.FIREWORK, beamPos.x, beamPos.y, beamPos.z, 
                        1, 0.08, 0.08, 0.08, 0.01);
            }
        }
        
        // Damage target every 4 ticks
        if (tick % 4 == 0 && distance <= BEAM_RANGE) {
            float damage = DAMAGE;
            // Extra damage to undead (zombies, skeletons, etc.)
            if (target.getType().getTranslationKey().contains("zombie") || 
                target.getType().getTranslationKey().contains("skeleton") ||
                target.getType().getTranslationKey().contains("wither") ||
                target.getType().getTranslationKey().contains("phantom")) {
                damage *= UNDEAD_DAMAGE_MULTIPLIER;
            }
            target.damage(world.getDamageSources().mobAttack(mob), damage);
            
            // Impact particles
            for (int i = 0; i < 5; i++) {
                world.spawnParticles(ParticleTypes.FIREWORK, targetPos.x, targetPos.y, targetPos.z, 
                        2, 0.3, 0.3, 0.3, 0.1);
            }
            
            world.playSound(null, targetPos.x, targetPos.y, targetPos.z, 
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, mob.getSoundCategory(), 0.3F, 1.5F);
        }
        
        // Continue beam on next tick
        world.getServer().execute(() -> {
            fireBeam(world, mob, target, startPos, tick + 1);
        });
    }
}

