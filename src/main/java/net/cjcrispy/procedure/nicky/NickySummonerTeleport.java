package net.cjcrispy.procedure.nicky;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class NickySummonerTeleport {
    private static final double MIN_TELEPORT_DISTANCE = 10.0; // Minimum distance to teleport
    private static final double MAX_TELEPORT_DISTANCE = 20.0; // Maximum distance to teleport
    private static final int TELEPORT_ATTEMPTS = 20; // Max attempts to find valid position
    private static final int SEARCH_RANGE_Y = 10; // How many blocks up/down to search for valid position
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        
        // Stop movement
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);
        
        // Play teleport preparation sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 
                mob.getSoundCategory(), 1.0F, 0.8F);
        
        // Teleport particles at current location
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 0.5 + mob.getRandom().nextDouble() * 0.5;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + mob.getRandom().nextDouble() * mob.getHeight();
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
        }
        
        // Delay teleport slightly for visual effect
        world.getServer().execute(() -> {
            Vec3d targetPos = target.getPos();
            Vec3d mobPos = mob.getPos();
            
            // Calculate direction away from target
            Vec3d awayFromTarget = mobPos.subtract(targetPos).normalize();
            
            // Try to find a valid teleport position
            Vec3d teleportPos = findTeleportPosition(world, mob, targetPos, awayFromTarget);
            
            if (teleportPos != null) {
                // Teleport the mob
                mob.teleport(teleportPos.x, teleportPos.y, teleportPos.z, false);
                
                // Play teleport arrival sound
                world.playSound(null, BlockPos.ofFloored(teleportPos), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 
                        mob.getSoundCategory(), 1.0F, 1.2F);
                
                // Teleport particles at new location
                for (int i = 0; i < 30; i++) {
                    double angle = (i / 30.0) * Math.PI * 2;
                    double radius = 0.5 + mob.getRandom().nextDouble() * 0.5;
                    double x = teleportPos.x + Math.cos(angle) * radius;
                    double y = teleportPos.y + mob.getRandom().nextDouble() * mob.getHeight();
                    double z = teleportPos.z + Math.sin(angle) * radius;
                    world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
                }
                
                // Make sure mob looks at target after teleport
                mob.getLookControl().lookAt(target, 30.0F, 30.0F);
            }
        });
    }
    
    private static Vec3d findTeleportPosition(ServerWorld world, MobEntity mob, Vec3d targetPos, Vec3d awayDirection) {
        double mobHeight = mob.getHeight();
        double mobWidth = Math.max(mob.getWidth(), 0.6); // Minimum width check
        
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            // Random distance between min and max
            double distance = MIN_TELEPORT_DISTANCE + mob.getRandom().nextDouble() * 
                             (MAX_TELEPORT_DISTANCE - MIN_TELEPORT_DISTANCE);
            
            // Add some randomness to the direction (not perfectly away)
            double angleOffset = (mob.getRandom().nextDouble() - 0.5) * Math.PI * 0.5; // ±45 degrees
            Vec3d direction = rotateVector(awayDirection, angleOffset);
            
            // Calculate potential teleport position
            double x = targetPos.x + direction.x * distance;
            double z = targetPos.z + direction.z * distance;
            
            // Start searching from target's Y level
            int startY = (int) targetPos.y;
            
            // Search up and down for a valid position
            for (int yOffset = -SEARCH_RANGE_Y; yOffset <= SEARCH_RANGE_Y; yOffset++) {
                int testY = startY + yOffset;
                BlockPos testPos = BlockPos.ofFloored(x, testY, z);
                
                // Check if this position is safe
                if (isSafeTeleportPosition(world, testPos, mobHeight, mobWidth)) {
                    // Found a safe position
                    return new Vec3d(testPos.getX() + 0.5, testPos.getY(), testPos.getZ() + 0.5);
                }
            }
        }
        
        // Fallback: try to find any safe position in a wider area
        for (int fallbackAttempt = 0; fallbackAttempt < 10; fallbackAttempt++) {
            double fallbackDistance = MIN_TELEPORT_DISTANCE + mob.getRandom().nextDouble() * 10.0;
            double angle = mob.getRandom().nextDouble() * Math.PI * 2; // Random direction
            double fallbackX = targetPos.x + Math.cos(angle) * fallbackDistance;
            double fallbackZ = targetPos.z + Math.sin(angle) * fallbackDistance;
            
            int startY = (int) targetPos.y;
            for (int yOffset = -SEARCH_RANGE_Y; yOffset <= SEARCH_RANGE_Y; yOffset++) {
                int testY = startY + yOffset;
                BlockPos testPos = BlockPos.ofFloored(fallbackX, testY, fallbackZ);
                
                if (isSafeTeleportPosition(world, testPos, mobHeight, mobWidth)) {
                    return new Vec3d(testPos.getX() + 0.5, testPos.getY(), testPos.getZ() + 0.5);
                }
            }
        }
        
        // Last resort: try to teleport to a position above the target (usually safe)
        BlockPos lastResort = BlockPos.ofFloored(targetPos.x, targetPos.y + 5, targetPos.z);
        if (isSafeTeleportPosition(world, lastResort, mobHeight, mobWidth)) {
            return new Vec3d(lastResort.getX() + 0.5, lastResort.getY(), lastResort.getZ() + 0.5);
        }
        
        // If all else fails, return null (teleport won't happen)
        return null;
    }
    
    private static boolean isSafeTeleportPosition(ServerWorld world, BlockPos pos, double mobHeight, double mobWidth) {
        // Check the main position block
        if (!world.getBlockState(pos).isAir()) {
            return false; // Position is blocked
        }
        
        // Check blocks around the entity (for width)
        int widthCheck = (int) Math.ceil(mobWidth / 2.0);
        for (int xOffset = -widthCheck; xOffset <= widthCheck; xOffset++) {
            for (int zOffset = -widthCheck; zOffset <= widthCheck; zOffset++) {
                BlockPos checkPos = pos.add(xOffset, 0, zOffset);
                if (!world.getBlockState(checkPos).isAir()) {
                    return false; // Entity would be inside a block
                }
            }
        }
        
        // Check headroom (need space for entity height)
        int heightCheck = (int) Math.ceil(mobHeight);
        for (int yOffset = 1; yOffset <= heightCheck; yOffset++) {
            BlockPos headPos = pos.up(yOffset);
            if (!world.getBlockState(headPos).isAir()) {
                return false; // Not enough headroom
            }
            
            // Also check width at head level
            for (int xOffset = -widthCheck; xOffset <= widthCheck; xOffset++) {
                for (int zOffset = -widthCheck; zOffset <= widthCheck; zOffset++) {
                    BlockPos headCheckPos = headPos.add(xOffset, 0, zOffset);
                    if (!world.getBlockState(headCheckPos).isAir()) {
                        return false;
                    }
                }
            }
        }
        
        // Check if there's solid ground below (prefer standing on ground)
        BlockPos belowPos = pos.down();
        if (!world.getBlockState(belowPos).isAir()) {
            return true; // Standing on solid ground - perfect!
        }
        
        // Also allow floating positions if there's enough space
        // Check a few blocks below to see if there's any solid ground nearby
        for (int yCheck = 1; yCheck <= 3; yCheck++) {
            BlockPos checkBelow = pos.down(yCheck);
            if (!world.getBlockState(checkBelow).isAir()) {
                return true; // There's ground below (floating is okay)
            }
        }
        
        // If we're floating too high, it's not safe
        return false;
    }
    
    private static Vec3d rotateVector(Vec3d vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(
            vector.x * cos - vector.z * sin,
            vector.y,
            vector.x * sin + vector.z * cos
        );
    }
}

