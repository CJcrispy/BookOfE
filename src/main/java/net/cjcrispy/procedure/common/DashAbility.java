package net.cjcrispy.procedure.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

public class DashAbility {

    public static void execute(Entity entity) {
        if (entity == null) return;

        if (entity instanceof MobEntity mob && mob.getTarget() instanceof LivingEntity target) {
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();

            double distance = Math.sqrt(dx * dx + dz * dz);

            // Define dash strength (higher value = farther dash)
            double dashSpeed = 1.5;

            // Determine whether to dash toward or away
            Vec3d dashVector;
            if (distance > 5.0) {
                // Dash TOWARD the target if further than 5 blocks
                dashVector = new Vec3d(dx, 0, dz).normalize().multiply(dashSpeed);
            } else {
                // Dash AWAY from the target if closer than 5 blocks
                dashVector = new Vec3d(-dx, 0, -dz).normalize().multiply(dashSpeed);
            }

            // Apply velocity
            mob.setVelocity(dashVector);
            mob.velocityModified = true; // En
        }
    }
}
