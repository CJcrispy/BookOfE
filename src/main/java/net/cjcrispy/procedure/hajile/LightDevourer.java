package net.cjcrispy.procedure.hajile;

import net.cjcrispy.entity.custom.KingHajileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class LightDevourer {
    private static final int CHARGE_DURATION = 100; // 5 seconds charging
    private static final int BEAM_DURATION = 100; // 5 seconds sweeping beam
    private static final double BEAM_RANGE = 40.0;
    private static final float DAMAGE = 20.0f;
    private static final int RADIANT_BURN_DURATION = 60; // 3 seconds
    
    public static void execute(Entity entity) {
        if (!(entity instanceof KingHajileEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Activate Light Devourer
        mob.setLightDevourerActive(true);
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, 
                mob.getSoundCategory(), 1.0F, 0.5F);
        
        // Start charging phase
        world.getServer().execute(() -> {
            chargePhase(world, mob, 0);
        });
    }
    
    private static void chargePhase(ServerWorld world, KingHajileEntity mob, int tick) {
        if (tick >= CHARGE_DURATION || !mob.isAlive()) {
            // Start beam phase
            world.getServer().execute(() -> {
                beamPhase(world, mob, 0, 0.0);
            });
            return;
        }
        
        // Brightness drops (particle effect to simulate darkness)
        if (tick % 10 == 0) {
            // Spawn dark particles around arena
            for (int i = 0; i < 50; i++) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = mob.getRandom().nextDouble() * BEAM_RANGE;
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY() + mob.getRandom().nextDouble() * 5;
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 3, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        // Pulsing glow in chest
        double pulseIntensity = Math.sin(tick * 0.2) * 0.5 + 0.5;
        for (int i = 0; i < (int)(20 * pulseIntensity); i++) {
            world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1.5, mob.getZ(), 
                    (int)(10 * pulseIntensity), 0.3, 0.3, 0.3, 0.1);
            world.spawnParticles(ParticleTypes.FIREWORK, mob.getX(), mob.getY() + 1.5, mob.getZ(), 
                    (int)(5 * pulseIntensity), 0.2, 0.2, 0.2, 0.05);
        }
        
        // Inhale orbs/light sources (visual effect - particles drawn toward Hajile)
        if (tick % 5 == 0) {
            for (int i = 0; i < 30; i++) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = mob.getRandom().nextDouble() * BEAM_RANGE;
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY() + mob.getRandom().nextDouble() * 5;
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                // Particles drawn toward Hajile
                Vec3d direction = mob.getPos().subtract(new Vec3d(x, y, z)).normalize();
                double progress = (tick / (double) CHARGE_DURATION) * 0.5;
                Vec3d particlePos = new Vec3d(x, y, z).add(direction.multiply(progress * radius * 0.5));
                
                world.spawnParticles(ParticleTypes.FIREWORK, particlePos.x, particlePos.y, particlePos.z, 
                        1, 0.1, 0.1, 0.1, 0.03);
                world.spawnParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z, 
                        1, 0.08, 0.08, 0.08, 0.02);
            }
        }
        
        // Continue charging
        world.getServer().execute(() -> {
            chargePhase(world, mob, tick + 1);
        });
    }
    
    private static void beamPhase(ServerWorld world, KingHajileEntity mob, int tick, double beamAngle) {
        if (tick >= BEAM_DURATION || !mob.isAlive()) {
            // Beam phase complete - Hajile staggers
            mob.setLightDevourerActive(false);
            
            // Stagger effect (vulnerable)
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2, false, false));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1, false, false));
            
            // Stagger particles
            for (int i = 0; i < 30; i++) {
                world.spawnParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 
                        5, 0.5, 0.5, 0.5, 0.1);
            }
            
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_HURT, 
                    mob.getSoundCategory(), 1.0F, 0.8F);
            return;
        }
        
        // Sweeping beam like a spotlight
        double sweepProgress = (tick / (double) BEAM_DURATION) * Math.PI * 2; // Full rotation
        double beamAngleCurrent = beamAngle + sweepProgress;
        
        // Beam extends from Hajile
        for (int i = 0; i < (int) BEAM_RANGE; i++) {
            double distance = i;
            double x = mob.getX() + Math.cos(beamAngleCurrent) * distance;
            double z = mob.getZ() + Math.sin(beamAngleCurrent) * distance;
            double y = mob.getY() + 1.5;
            
            // Beam particles (spotlight effect)
            for (int j = 0; j < 3; j++) {
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0.3, 0.3, 0.3, 0.05);
            }
            
            // Damage entities in beam
            Box beamBox = new Box(x - 2, y - 1, z - 2, x + 2, y + 2, z + 2);
            
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, beamBox, 
                    e -> e != mob && e instanceof PlayerEntity)) {
                // Only damage once per tick
                if (tick % 5 == 0) {
                    entity.damage(world.getDamageSources().mobAttack(mob), DAMAGE);
                    
                    // Radiant Burn debuff
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, RADIANT_BURN_DURATION, 0, false, false));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1, false, false));
                    
                    // Impact effect
                    for (int k = 0; k < 5; k++) {
                        world.spawnParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1, entity.getZ(), 
                                2, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
        
        // Beam sound
        if (tick % 20 == 0) {
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
                    mob.getSoundCategory(), 0.5F, 1.0F);
        }
        
        // Continue beam
        world.getServer().execute(() -> {
            beamPhase(world, mob, tick + 1, beamAngle);
        });
    }
}

