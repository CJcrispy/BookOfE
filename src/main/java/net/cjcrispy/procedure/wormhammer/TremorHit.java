package net.cjcrispy.procedure.wormhammer;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TremorHit {
    private static final double SHOCKWAVE_RADIUS = 3.0;
    private static final float SHOCKWAVE_DAMAGE = 4.0f;
    private static final double KNOCKBACK_STRENGTH = 0.5;
    
    public static void execute(PlayerEntity player, LivingEntity hitTarget) {
        if (player == null || player.getWorld().isClient()) return;
        
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d playerPos = player.getPos();
        
        // Play sound
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z, 
            SoundEvents.ENTITY_GENERIC_EXPLODE, player.getSoundCategory(), 0.5f, 1.5f);
        
        // Spawn particles
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double x = playerPos.x + Math.cos(angle) * SHOCKWAVE_RADIUS * 0.5;
            double z = playerPos.z + Math.sin(angle) * SHOCKWAVE_RADIUS * 0.5;
            world.spawnParticles(ParticleTypes.EXPLOSION, x, playerPos.y, z, 1, 0, 0, 0, 0.1);
        }
        
        // Damage and knockback nearby entities
        Box aoeBox = new Box(
            playerPos.x - SHOCKWAVE_RADIUS, playerPos.y - 1, playerPos.z - SHOCKWAVE_RADIUS,
            playerPos.x + SHOCKWAVE_RADIUS, playerPos.y + 2, playerPos.z + SHOCKWAVE_RADIUS
        );
        
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, aoeBox, e -> e != player)) {
            Vec3d direction = entity.getPos().subtract(playerPos).normalize();
            direction = new Vec3d(direction.x, 0.3, direction.z).normalize();
            
            // Apply damage
            entity.damage(world.getDamageSources().playerAttack(player), SHOCKWAVE_DAMAGE);
            
            // Apply knockback
            entity.addVelocity(direction.multiply(KNOCKBACK_STRENGTH));
            entity.velocityModified = true;
        }
    }
}

