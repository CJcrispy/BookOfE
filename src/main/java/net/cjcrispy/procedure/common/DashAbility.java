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
            double dy = target.getY() - entity.getY();
            double dz = target.getZ() - entity.getZ();

            // Apply dash movement vector
            Vec3d dashVector = new Vec3d(
                    (dx * 0.35) - (dx * 0.05),
                    (dy * 0.35) - (dy * 0.05) + 0.15,
                    (dz * 0.35) - (dz * 0.05)
            );

            entity.setVelocity(dashVector);
            entity.velocityModified = true; // Ensures movement is applied
        }
    }
}
