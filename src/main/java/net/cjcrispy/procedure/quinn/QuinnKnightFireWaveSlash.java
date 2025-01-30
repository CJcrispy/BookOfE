package net.cjcrispy.procedure.quinn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class QuinnKnightFireWaveSlash {
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getTarget() == null) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        LivingEntity target = mob.getTarget();

        // ⚡ Telegraph: Fire particles swirl around QuinnKnight before slashing
        for (int i = 0; i < 10; i++) {
            world.spawnParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 1, mob.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        }

        // Wait 20 ticks (1 second) before attacking
        world.getServer().execute(() -> {
            Vec3d direction = target.getPos().subtract(mob.getPos()).normalize();

            // 🔥 Create the Fireball Entity with correct constructor
            FireballEntity fireball = new FireballEntity(world, mob, direction, 2); // Explosion power set to 2

            fireball.setPosition(mob.getX(), mob.getY() + 1.5, mob.getZ()); // Position fireball correctly
            fireball.setOwner(mob); // Set QuinnKnight as the fireball's owner

            world.spawnEntity(fireball); // Spawn the fireball

            // 🔥 Apply fire effect on nearby enemies
            for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, mob.getBoundingBox().expand(3), e -> e != mob)) {
                nearby.damage(world.getDamageSources().mobAttack(mob), 8);
                nearby.setOnFireFor(5); // Set nearby enemies on fire for 5 seconds
            }
        });
    }
}
