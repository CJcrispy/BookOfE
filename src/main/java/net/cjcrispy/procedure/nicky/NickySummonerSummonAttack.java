package net.cjcrispy.procedure.nicky;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class NickySummonerSummonAttack {
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Trigger evoker casting animation by sending entity status
        // Status 4 is used by evokers for their casting animation
        mob.getWorld().sendEntityStatus(mob, (byte) 4);
        
        // Play evoker casting sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 
                mob.getSoundCategory(), 1.0F, 1.0F);

        // Summoning particles around the mob
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 1.5;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1.0;
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0, 0.1, 0, 0.05);
            world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0, 0.1, 0, 0.05);
        }

        // Delay the actual summoning to match animation timing
        world.getServer().execute(() -> {
            // Summon 2-3 BlackBird entities around Nicky
            int summonCount = 2 + mob.getRandom().nextInt(2); // 2 or 3 summons
            
            for (int i = 0; i < summonCount; i++) {
                double angle = (i / (double) summonCount) * Math.PI * 2;
                double radius = 2.0 + mob.getRandom().nextDouble() * 1.5;
                double x = mob.getX() + Math.cos(angle) * radius;
                double y = mob.getY();
                double z = mob.getZ() + Math.sin(angle) * radius;
                
                BlockPos spawnPos = BlockPos.ofFloored(x, y, z);
                
                // Find a valid spawn position (air block)
                while (!world.getBlockState(spawnPos).isAir() && spawnPos.getY() < mob.getY() + 3) {
                    spawnPos = spawnPos.up();
                }
                
                // Create and spawn BlackBird
                BlackBirdEntity summoned = ModEntities.BLACKBIRD_WARRIOR.create(world);
                if (summoned != null) {
                    summoned.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 
                            mob.getRandom().nextFloat() * 360.0F, 0.0F);
                    
                    // Set the same target as Nicky
                    if (mob.getTarget() != null) {
                        summoned.setTarget(mob.getTarget());
                    }
                    
                    world.spawnEntity(summoned);
                    
                    // Summon particles at spawn location
                    for (int j = 0; j < 10; j++) {
                        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, 
                                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, 
                                3, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
            
            // Play summon completion sound
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 
                    mob.getSoundCategory(), 1.0F, 1.0F);
        });
    }
}

