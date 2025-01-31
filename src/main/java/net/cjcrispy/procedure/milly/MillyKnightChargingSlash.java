package net.cjcrispy.procedure.milly;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class MillyKnightChargingSlash {

    public static void execute(Entity entity) {
        if (entity == null ) return;

        if (entity instanceof MobEntity mob && mob.getTarget() instanceof LivingEntity target) {
            // Set up charge time (could be based on mob state)
            int chargeTime = 20; // 1 second charge

            // Charge up: Play an animation or particle effect (this can be added if needed)
            mob.getWorld().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, mob.getSoundCategory(), 1.0F, 1.0F);

            // Wait for charge to finish (using a timer or tick counter)
            mob.getWorld().getServer().execute(() -> {
                // Once the charge is complete, apply the charging slash attack
                double dx = target.getX() - mob.getX();
                double dy = target.getY() - mob.getY();
                double dz = target.getZ() - mob.getZ();

                // Apply a forward slash movement
                Vec3d slashDirection = new Vec3d(dx, dy, dz).normalize().multiply(2.0); // Modify speed as needed

                mob.setVelocity(slashDirection);
                mob.velocityModified = true; // Ensures the movement is applied

                // Apply damage to target using the generic damage source
                if (target.squaredDistanceTo(mob) < 16.0F) { // Check if target is in range


//                    // Use the generic damage source from DamageSources
//                    DamageSource damageSource = damageSources.generic();
//
//                    target.damage(damageSource, 10.0F); // Modify damage as needed
                    mob.swingHand(Hand.MAIN_HAND); // Trigger the slash animation
                }

                // Optional: You can add a particle effect for the slash if needed
//                mob.getWorld().spawnParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY(), mob.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }); // Execute after charge time
        }

    }
}

