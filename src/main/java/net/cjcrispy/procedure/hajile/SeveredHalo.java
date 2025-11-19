package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class SeveredHalo {
    private static final double HALO_RANGE = 20.0;
    private static final double HALO_SPEED = 1.5;
    private static final float DAMAGE = 12.0f;
    private static final double KNOCKBACK = 0.4;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Halo formation particles
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 1.0;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 2;
            double z = mob.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 3, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.15, 0.15, 0.15, 0.05);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, 
                mob.getSoundCategory(), 1.0F, 0.5F);
        
        // Wait 10 ticks before throwing
        world.getServer().execute(() -> {
            throwHalo(world, mob, target, 0);
        });
    }
    
    private static void throwHalo(ServerWorld world, MobEntity mob, LivingEntity target, int tick) {
        if (tick >= 80 || !mob.isAlive() || target == null || !target.isAlive()) { // 4 seconds total
            return;
        }
        
        Vec3d mobPos = mob.getPos().add(0, 2, 0);
        Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
        
        double progress = (tick / 80.0) * Math.PI * 2; // Full circle
        double currentRadius;
        
        if (tick < 40) {
            // Outward throw
            currentRadius = (tick / 40.0) * HALO_RANGE;
        } else {
            // Return to Hajile
            currentRadius = HALO_RANGE - ((tick - 40) / 40.0) * HALO_RANGE;
        }
        
        Vec3d direction = targetPos.subtract(mobPos).normalize();
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x).normalize();
        
        // Halo position (orbits around target on outward, returns on inward)
        Vec3d haloPos;
        if (tick < 40) {
            // Orbit outward
            double angle = progress * 2; // Faster rotation
            haloPos = targetPos.add(perpendicular.multiply(Math.cos(angle) * currentRadius))
                    .add(new Vec3d(0, Math.sin(angle * 0.5) * 2, 0));
        } else {
            // Return to Hajile
            double returnProgress = (tick - 40) / 40.0;
            haloPos = targetPos.add(perpendicular.multiply(Math.cos(progress * 2) * currentRadius))
                    .add(new Vec3d(0, Math.sin(progress) * 2, 0))
                    .multiply(1 - returnProgress)
                    .add(mobPos.multiply(returnProgress));
        }
        
        // Halo particles
        for (int i = 0; i < 10; i++) {
            double angle = (i / 10.0) * Math.PI * 2;
            double radius = 0.5;
            double x = haloPos.x + Math.cos(angle) * radius;
            double y = haloPos.y;
            double z = haloPos.z + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.08, 0.08, 0.08, 0.03);
        }
        
        // Damage entities near halo
        net.minecraft.util.math.Box haloBox = new net.minecraft.util.math.Box(
                haloPos.x - 1, haloPos.y - 1, haloPos.z - 1,
                haloPos.x + 1, haloPos.y + 1, haloPos.z + 1
        );
        
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, haloBox, 
                e -> e != mob && e.squaredDistanceTo(haloPos) <= 1.5)) {
            entity.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
            
            // Knockback
            Vec3d knockbackDir = entity.getPos().subtract(haloPos).normalize();
            entity.addVelocity(knockbackDir.x * KNOCKBACK, 0.2, knockbackDir.z * KNOCKBACK);
            entity.velocityModified = true;
            
            // Impact effect
            for (int i = 0; i < 5; i++) {
                world.spawnParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1, entity.getZ(), 
                        2, 0.3, 0.3, 0.3, 0.1);
            }
            
            world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 
                    mob.getSoundCategory(), 0.8F, 1.5F);
        }
        
        // Continue halo
        world.getServer().execute(() -> {
            throwHalo(world, mob, target, tick + 1);
        });
    }
}

