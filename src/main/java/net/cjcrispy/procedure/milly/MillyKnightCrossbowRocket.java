package net.cjcrispy.procedure.milly;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class MillyKnightCrossbowRocket {

    public static void execute(Entity entity) {
        if (entity == null || !(entity instanceof MobEntity mob)) return;
        if (!(mob.getTarget() instanceof LivingEntity target)) return;

        // Make the mob stationary while firing
        mob.getNavigation().stop();
        mob.setVelocity(Vec3d.ZERO);

        // Particle effect to indicate firing
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1, entity.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        }

        // Fire 6 rockets in a burst with short delays
        for (int i = 0; i < 6; i++) {
            int delay = i * 5; // Each rocket fires every 5 ticks
            entity.getWorld().getServer().execute(() -> fireRocket(entity));
            try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
        }
    }

    private static void fireRocket(Entity entity) {
        if (entity.getWorld().isClient) return;

        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkRocketEntity rocket = new FireworkRocketEntity(entity.getWorld(), entity.getX(), entity.getY() + 1, entity.getZ(), rocketItem);
        rocket.setVelocity(entity.getRotationVector().multiply(1.5));
        entity.getWorld().spawnEntity(rocket);
    }
}
