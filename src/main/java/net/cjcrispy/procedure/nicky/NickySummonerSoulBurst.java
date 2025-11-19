package net.cjcrispy.procedure.nicky;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NickySummonerSoulBurst {
    private static final int MARK_COUNT_MIN = 3;
    private static final int MARK_COUNT_MAX = 6;
    private static final double MARK_RANGE = 8.0; // 8 blocks around target
    private static final double EXPLOSION_RADIUS = 2.5; // Explosion radius
    private static final float EXPLOSION_DAMAGE = 8.0f;
    private static final int DELAY_TICKS = 60; // 3 seconds delay (20 ticks per second)
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement to cast
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Play casting sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 
                mob.getSoundCategory(), 1.0F, 0.9F);
        
        // Casting particles around Nicky
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double radius = 1.0;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1.0;
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.SOUL, x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
        }
        
        // Determine number of marks (3-6)
        int markCount = MARK_COUNT_MIN + mob.getRandom().nextInt(MARK_COUNT_MAX - MARK_COUNT_MIN + 1);
        
        // Generate random positions around the target
        List<Vec3d> markPositions = new ArrayList<>();
        Vec3d targetPos = target.getPos();
        
        for (int i = 0; i < markCount; i++) {
            // Random angle and distance
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            double distance = 3.0 + mob.getRandom().nextDouble() * (MARK_RANGE - 3.0);
            
            double x = targetPos.x + Math.cos(angle) * distance;
            double y = targetPos.y; // Ground level
            double z = targetPos.z + Math.sin(angle) * distance;
            
            // Find the ground position (first non-air block below)
            BlockPos groundPos = BlockPos.ofFloored(x, y, z);
            while (groundPos.getY() > targetPos.y - 5 && world.getBlockState(groundPos).isAir()) {
                groundPos = groundPos.down();
            }
            // Move up one block to be on top of the ground
            groundPos = groundPos.up();
            
            markPositions.add(new Vec3d(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5));
        }
        
        // Show telegraphs immediately
        showTelegraphs(world, markPositions, 0);
        
        // Explode after delay
        scheduleExplosion(world, mob, markPositions, 0);
    }
    
    private static void scheduleExplosion(ServerWorld world, MobEntity caster, List<Vec3d> positions, int tick) {
        if (tick >= DELAY_TICKS) {
            explodeMarks(world, caster, positions);
            return;
        }
        
        world.getServer().execute(() -> {
            scheduleExplosion(world, caster, positions, tick + 1);
        });
    }
    
    private static void showTelegraphs(ServerWorld world, List<Vec3d> positions, int tick) {
        if (tick >= DELAY_TICKS) {
            return;
        }
        
        for (Vec3d pos : positions) {
            // Create a glyph/ring pattern on the ground
            double progress = tick / (double) DELAY_TICKS;
            
            // Outer ring particles
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * Math.PI * 2;
                double radius = EXPLOSION_RADIUS * (0.7 + progress * 0.3);
                double x = pos.x + Math.cos(angle) * radius;
                double y = pos.y + 0.1;
                double z = pos.z + Math.sin(angle) * radius;
                
                // Pulsing effect - more intense as time progresses
                int particleCount = tick % 10 < 5 ? 1 : 2;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, particleCount, 0, 0, 0, 0.02);
            }
            
            // Center marker
            world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.1, pos.z, 2, 0.2, 0, 0.2, 0.05);
            
            // Rising particles
            if (tick % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    double offsetX = (world.getRandom().nextDouble() - 0.5) * 0.5;
                    double offsetZ = (world.getRandom().nextDouble() - 0.5) * 0.5;
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, 
                            pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ, 
                            1, 0, 0.1, 0, 0.05);
                }
            }
        }
        
        // Continue showing telegraphs
        world.getServer().execute(() -> {
            showTelegraphs(world, positions, tick + 1);
        });
    }
    
    private static void explodeMarks(ServerWorld world, MobEntity caster, List<Vec3d> positions) {
        // Play warning sound
        for (Vec3d pos : positions) {
            world.playSound(null, BlockPos.ofFloored(pos), SoundEvents.ENTITY_WITHER_SHOOT, 
                    caster.getSoundCategory(), 0.5F, 1.5F);
        }
        
        // Small delay before explosions
        world.getServer().execute(() -> {
            for (Vec3d pos : positions) {
                // Create explosion
                world.createExplosion(caster, pos.x, pos.y, pos.z, 2.0F, World.ExplosionSourceType.MOB);
                
                // Find entities in explosion radius and damage them
                Box damageBox = new Box(
                        pos.x - EXPLOSION_RADIUS, pos.y - EXPLOSION_RADIUS, pos.z - EXPLOSION_RADIUS,
                        pos.x + EXPLOSION_RADIUS, pos.y + EXPLOSION_RADIUS, pos.z + EXPLOSION_RADIUS
                );
                
                world.getEntitiesByClass(LivingEntity.class, damageBox, entity -> {
                    double distance = pos.distanceTo(entity.getPos());
                    // Don't damage caster or BlackBird minions
                    return distance <= EXPLOSION_RADIUS && entity != caster && 
                           !(entity instanceof net.cjcrispy.entity.custom.BlackBirdEntity);
                }).forEach(entity -> {
                    // Damage
                    entity.damage(world.getDamageSources().mobAttack(caster), EXPLOSION_DAMAGE);
                    
                    // Knockback away from explosion center
                    Vec3d knockbackDir = entity.getPos().subtract(pos).normalize();
                    Vec3d knockback = knockbackDir.multiply(1.2);
                    entity.addVelocity(knockback.x, 0.3, knockback.z);
                    entity.velocityModified = true;
                    
                    // Additional particles on hit
                    for (int i = 0; i < 5; i++) {
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, 
                                entity.getX(), entity.getY() + 0.5, entity.getZ(), 
                                2, 0.3, 0.3, 0.3, 0.1);
                    }
                });
                
                // Explosion particles
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * Math.PI * 2;
                    double radius = EXPLOSION_RADIUS * world.getRandom().nextDouble();
                    double x = pos.x + Math.cos(angle) * radius;
                    double y = pos.y + world.getRandom().nextDouble() * 2;
                    double z = pos.z + Math.sin(angle) * radius;
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0, 0, 0, 0.1);
                    world.spawnParticles(ParticleTypes.SOUL, x, y, z, 1, 0, 0, 0, 0.05);
                }
            }
        });
    }
}

