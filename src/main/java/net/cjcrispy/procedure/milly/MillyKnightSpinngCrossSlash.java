package net.cjcrispy.procedure.milly;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class MillyKnightSpinngCrossSlash {

    public static void execute(Entity entity) {
        if (entity == null || !(entity instanceof MobEntity mob)) return;
        if (!(mob.getTarget() instanceof LivingEntity target)) return;

        // Stop movement and set spinning animation
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);

        // Create particle effect to indicate spinning slash
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 10; i++) {
                serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, entity.getX(), entity.getY() + 1, entity.getZ(), 5, 1.0, 0.5, 1.0, 0.1);
            }
        }

        // Find entities around mob and apply damage + knockback
        Box attackArea = new Box(entity.getX() - 2, entity.getY() - 1, entity.getZ() - 2,
                entity.getX() + 2, entity.getY() + 2, entity.getZ() + 2);
        List<LivingEntity> nearbyEntities = entity.getWorld().getEntitiesByClass(LivingEntity.class, attackArea, e -> e != entity);

        for (LivingEntity e : nearbyEntities) {
            e.damage(entity.getDamageSources().mobAttack(mob), 8.0f); // 8 damage
            Vec3d knockback = new Vec3d(e.getX() - entity.getX(), 0, e.getZ() - entity.getZ()).normalize().multiply(1.5);
            e.addVelocity(knockback.x, 0.2, knockback.z); // Push enemies away
        }
    }
}
