package net.cjcrispy.procedure.quinn;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class QuinnKnightLeapingSlam {
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();
        BlockPos targetPos = target.getBlockPos();

        // ⚡ Telegraph: Particles around QuinnKnight before jumping
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY(), mob.getZ(), 3, 0.5, 0.5, 0.5, 0.05);
        }

        // Jump after 1 second (20 ticks)
        world.getServer().execute(() -> {
            mob.addVelocity(0, 1, 0);
            mob.velocityModified = true;

            // ⚡ Telegraph: Particles at target impact location
            for (int i = 0; i < 20; i++) {
                world.spawnParticles(ParticleTypes.FLAME, targetPos.getX(), targetPos.getY(), targetPos.getZ(), 10, 1, 0, 1, 0.1);
            }

            // Slam Down After Another 1 Second (40 Ticks)
            world.getServer().execute(() -> {
                mob.setVelocity(0, -1.5, 0);
                mob.velocityModified = true;

                // Damage & Fire Explosion
                for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, mob.getBoundingBox().expand(3), e -> e != mob)) {
                    nearby.damage(mob.getWorld().getDamageSources().mobAttack(mob), 10);
                    nearby.setOnFireFor(3);
                }

                // Set fire on impact
                for (BlockPos pos : BlockPos.iterate(targetPos.add(-1, 0, -1), targetPos.add(1, 0, 1))) {
                    if (world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 3);
                    }
                }
            });
        });
    }
}
