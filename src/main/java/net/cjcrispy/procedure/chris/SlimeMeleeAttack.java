package net.cjcrispy.procedure.chris;

import net.cjcrispy.entity.custom.SlimeChrisEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class SlimeMeleeAttack {
    public static void execute(SlimeChrisEntity mob, LivingEntity target) {
        if (mob == null || target == null || !target.isAlive()) return;
        if (mob.getWorld().isClient()) return;

        // Ensure slime hammer is equipped
        mob.ensureSlimeHammerEquipped();

        // Swing hand
        mob.swingHand(Hand.MAIN_HAND);

        // Perform melee attack with extra knockback
        if (mob.isInAttackRange(target)) {
            mob.tryAttack(target);
            
            // Apply additional knockback
            Vec3d knockback = new Vec3d(
                    target.getX() - mob.getX(),
                    0.0,
                    target.getZ() - mob.getZ()
            ).normalize().multiply(1.5);
            target.addVelocity(knockback.x, 0.3, knockback.z);
            target.velocityModified = true;
        }
    }
}

