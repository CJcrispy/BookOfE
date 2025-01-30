package net.cjcrispy.procedure.quinn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class QuinnKnightSoulExplosion {
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        Vec3d position = mob.getPos(); // Explosion center at the mob's position

        // ⚡ Telegraph: Soul particles around QuinnKnight before explosion
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 1.5, mob.getZ(), 3, 0.5, 0.5, 0.5, 0.1);
        }

        // Delay the explosion using a server-side task
        world.getServer().execute(() -> {
            // ⚠️ Soul Explosion logic
            world.createExplosion(mob, position.getX(), position.getY(), position.getZ(), 4.0F, World.ExplosionSourceType.MOB);

            // Optionally add custom sound or further effects if needed
            world.playSound(null, position.getX(), position.getY(), position.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, mob.getSoundCategory(), 1.0F, 1.0F);
        }); // Delay of 40 ticks (approximately 2 seconds)
    }
}
