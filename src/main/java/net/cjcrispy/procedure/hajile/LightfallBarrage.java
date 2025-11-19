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

public class LightfallBarrage {
    private static final double BARRAGE_RANGE = 30.0;
    private static final int BEAM_COUNT = 8;
    private static final float DAMAGE = 10.0f;
    private static final int RADIANT_BURN_DURATION = 40; // 2 seconds
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Point skyward animation
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 2, mob.getZ(), 
                    5, 0.3, 0.5, 0.3, 0.1);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 
                mob.getSoundCategory(), 0.8F, 1.5F);
        
        // Wait 20 ticks before barrage
        world.getServer().execute(() -> {
            spawnBeams(world, mob, 0);
        });
    }
    
    private static void spawnBeams(ServerWorld world, MobEntity mob, int beamIndex) {
        if (beamIndex >= BEAM_COUNT || !mob.isAlive()) {
            return;
        }
        
        // Random positions in arena around Hajile
        double angle = (beamIndex / (double) BEAM_COUNT) * Math.PI * 2 + mob.getRandom().nextDouble() * 0.5;
        double radius = 5.0 + mob.getRandom().nextDouble() * BARRAGE_RANGE * 0.5;
        double targetX = mob.getX() + Math.cos(angle) * radius;
        double targetZ = mob.getZ() + Math.sin(angle) * radius;
        double targetY = mob.getY(); // Will find ground level
        
        // Find ground level
        net.minecraft.util.math.BlockPos.Mutable pos = new net.minecraft.util.math.BlockPos.Mutable(
                (int) targetX, (int) mob.getY() + 20, (int) targetZ);
        while (pos.getY() > mob.getY() - 10 && world.isAir(pos)) {
            pos.move(0, -1, 0);
        }
        targetY = pos.getY() + 1;
        
        // Telegraph beam (vertical line from sky)
        for (int i = 0; i < 20; i++) {
            double y = mob.getY() + 20 - i;
            world.spawnParticles(ParticleTypes.END_ROD, targetX, y, targetZ, 
                    2, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Wait 30 ticks (1.5 seconds) before strike
        int finalBeamIndex = beamIndex;
        double finalTargetX = targetX;
        double finalTargetY = targetY;
        double finalTargetZ = targetZ;
        
        world.getServer().execute(() -> {
            strikeBeam(world, mob, finalTargetX, finalTargetY, finalTargetZ, 0);
            
            // Spawn next beam after delay
            world.getServer().execute(() -> {
                spawnBeams(world, mob, finalBeamIndex + 1);
            });
        });
    }
    
    private static void strikeBeam(ServerWorld world, MobEntity mob, double x, double y, double z, int tick) {
        if (tick >= 20 || !mob.isAlive()) { // 1 second strike duration
            return;
        }
        
        // Vertical pillar of light
        for (int i = 0; i < 5; i++) {
            double pillarY = y + i;
            world.spawnParticles(ParticleTypes.END_ROD, x, pillarY, z, 
                    10, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticles(ParticleTypes.FIREWORK, x, pillarY, z, 
                    5, 0.3, 0.3, 0.3, 0.05);
            
            if (tick == 0 && i == 0) {
                world.spawnParticles(ParticleTypes.EXPLOSION, x, pillarY, z, 
                        3, 0.5, 0.5, 0.5, 0.2);
            }
        }
        
        // Damage entities in beam
        if (tick == 0) {
            Box beamBox = new Box(x - 1.5, y, z - 1.5, x + 1.5, y + 5, z + 1.5);
            
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, beamBox, 
                    e -> e != mob)) {
                entity.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
                
                // Apply Radiant Burn
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, RADIANT_BURN_DURATION, 0, false, false));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 0, false, false));
                
                // Knockback
                Vec3d direction = entity.getPos().subtract(new Vec3d(x, y, z)).normalize();
                entity.addVelocity(direction.x * 0.3, 0.3, direction.z * 0.3);
                entity.velocityModified = true;
            }
            
            world.playSound(null, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                    mob.getSoundCategory(), 0.5F, 1.8F);
        }
        
        // Continue beam effect
        world.getServer().execute(() -> {
            strikeBeam(world, mob, x, y, z, tick + 1);
        });
    }
}

