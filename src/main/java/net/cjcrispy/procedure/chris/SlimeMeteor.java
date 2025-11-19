package net.cjcrispy.procedure.chris;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SlimeMeteor {
    public static void execute(Entity entity, LivingEntity target) {
        if (!(entity instanceof MobEntity mob) || mob.getWorld().isClient()) return;
        if (target == null || !target.isAlive()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();

        // Calculate target position on ground
        BlockPos targetPos = target.getBlockPos();
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY() + 20; // Start 20 blocks above
        double targetZ = targetPos.getZ() + 0.5;

        // Spawn warning particles at target location
        for (int i = 0; i < 30; i++) {
            world.spawnParticles(ParticleTypes.ITEM_SLIME,
                    targetX, targetPos.getY() + 1, targetZ,
                    5, 2.0, 0.5, 2.0, 0.1);
        }

        // Play warning sound
        world.playSound(null, targetPos, SoundEvents.ENTITY_SLIME_JUMP,
                mob.getSoundCategory(), 2.0F, 0.5F);

        // Delay the meteor fall (1 second warning)
        world.getServer().execute(() -> {
            // Create a large slime entity that falls
            SlimeEntity meteorSlime = net.minecraft.entity.EntityType.SLIME.create(world);
            if (meteorSlime != null) {
                // Set size to 4 (largest slime size)
                meteorSlime.setSize(4, true);
                meteorSlime.refreshPositionAndAngles(targetX, targetY, targetZ, 0.0F, 0.0F);
                meteorSlime.setVelocity(0, -0.8, 0); // Make it fall faster
                meteorSlime.setNoGravity(false);
                world.spawnEntity(meteorSlime);

                // Calculate approximate fall time (20 blocks at ~0.8 blocks/tick = ~25 ticks)
                // Schedule impact after fall time + buffer
                int fallTicks = (int) ((targetY - targetPos.getY()) / 0.8) + 5;
                scheduleImpact(world, targetX, targetPos.getY() + 0.5, targetZ, mob, fallTicks);
            }
        });
    }

    private static void scheduleImpact(ServerWorld world, double x, double y, double z, MobEntity attacker, int ticksRemaining) {
        if (ticksRemaining <= 0) {
            impact(world, x, y, z, attacker);
            return;
        }

        // Schedule impact for next tick
        world.getServer().execute(() -> {
            scheduleImpact(world, x, y, z, attacker, ticksRemaining - 1);
        });
    }

    private static void impact(ServerWorld world, double x, double y, double z, MobEntity attacker) {
        // Explosion effect
        world.createExplosion(attacker, x, y, z, 3.0F, false, net.minecraft.world.World.ExplosionSourceType.NONE);

        // Damage nearby entities
        net.minecraft.util.math.Box impactBox = new net.minecraft.util.math.Box(
                x - 4, y - 1, z - 4,
                x + 4, y + 3, z + 4);
        for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, impactBox, e -> e != attacker)) {
            nearby.damage(world.getDamageSources().mobAttack(attacker), 15.0f);
            Vec3d knockback = new Vec3d(nearby.getX() - x, 0.5, nearby.getZ() - z).normalize().multiply(2.0);
            nearby.addVelocity(knockback.x, knockback.y, knockback.z);
        }

        // Impact particles
        for (int i = 0; i < 50; i++) {
            world.spawnParticles(ParticleTypes.ITEM_SLIME,
                    x, y, z, 10, 3.0, 1.0, 3.0, 0.2);
        }

        // Impact sound
        world.playSound(null, BlockPos.ofFloored(x, y, z),
                SoundEvents.ENTITY_SLIME_SQUISH,
                attacker.getSoundCategory(), 2.0F, 0.3F);
    }
}

