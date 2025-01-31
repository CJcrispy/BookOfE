package net.cjcrispy.procedure.milly;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class MillyKnightChainGrab {
    public static void execute(Entity entity) {
        if (entity == null || !(entity instanceof MobEntity mob)) return;
        if (!(mob.getTarget() instanceof LivingEntity target)) return;

        double distance = entity.squaredDistanceTo(target);

        // Only grab if target is within 6 blocks
        if (distance > 36) return;

        // Particle effect to represent the chain being thrown
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 10; i++) {
                double progress = i / 10.0;
                double chainX = entity.getX() + (target.getX() - entity.getX()) * progress;
                double chainY = entity.getY() + (target.getY() - entity.getY()) * progress + 1;
                double chainZ = entity.getZ() + (target.getZ() - entity.getZ()) * progress;
                serverWorld.spawnParticles(ParticleTypes.CRIT, chainX, chainY, chainZ, 1, 0, 0, 0, 0);
            }
        }

        // Pull target towards the mob
        Vec3d pullVector = new Vec3d(entity.getX() - target.getX(), 0, entity.getZ() - target.getZ()).normalize().multiply(1.5);
        target.addVelocity(pullVector.x, 0.3, pullVector.z);

        // Deal damage and apply fire effect
        target.damage(entity.getDamageSources().mobAttack(mob), 6.0f); // 6 damage
        target.setOnFireFor(3); // Burn for 3 seconds
    }
}
