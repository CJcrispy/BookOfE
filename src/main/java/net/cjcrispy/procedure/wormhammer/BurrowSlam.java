package net.cjcrispy.procedure.wormhammer;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BurrowSlam {
    private static final double LEAP_DISTANCE = 6.0;
    private static final double LEAP_HEIGHT = 0.8;
    private static final double SLAM_LINE_LENGTH = 8.0;
    private static final double SLAM_WIDTH = 1.5;
    private static final float SLAM_DAMAGE = 12.0f;
    private static final int SLOWNESS_DURATION = 60; // 3 seconds
    
    public static void execute(PlayerEntity player, ServerWorld world) {
        if (player == null || world == null) return;
        
        Vec3d playerPos = player.getPos();
        Vec3d lookVec = player.getRotationVector();
        Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
        
        // Leap forward
        Vec3d leapVelocity = horizontalLook.multiply(LEAP_DISTANCE * 0.2).add(0, LEAP_HEIGHT, 0);
        player.setVelocity(leapVelocity);
        player.velocityModified = true;
        
        // Play leap sound
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z, 
            SoundEvents.ENTITY_RAVAGER_ATTACK, player.getSoundCategory(), 1.0f, 0.8f);
        
        // Wait for player to land (approximately 0.8-1 second for the leap, ~20 ticks)
        // Use nested execute to add delay
        world.getServer().execute(() -> {
            // Wait another tick cycle for the slam
            world.getServer().execute(() -> {
            Vec3d landingPos = player.getPos();
            Vec3d slamDirection = new Vec3d(horizontalLook.x, 0, horizontalLook.z).normalize();
            
            // Play slam sound
            world.playSound(null, landingPos.x, landingPos.y, landingPos.z, 
                SoundEvents.ENTITY_GENERIC_EXPLODE, player.getSoundCategory(), 1.0f, 0.7f);
            
            // Create line of erupting dirt spikes
            for (int i = 0; i <= SLAM_LINE_LENGTH; i++) {
                Vec3d spikePos = landingPos.add(slamDirection.multiply(i));
                BlockPos blockPos = BlockPos.ofFloored(spikePos);
                
                // Spawn particles for the spike
                for (int j = 0; j < 10; j++) {
                    world.spawnParticles(ParticleTypes.CLOUD, 
                        spikePos.x, blockPos.getY() + 1, spikePos.z, 
                        5, 0.3, 0.5, 0.3, 0.1);
                    world.spawnParticles(ParticleTypes.POOF, 
                        spikePos.x, blockPos.getY() + 1, spikePos.z, 
                        3, 0.2, 0.3, 0.2, 0.05);
                    world.spawnParticles(ParticleTypes.EXPLOSION, 
                        spikePos.x, blockPos.getY() + 1, spikePos.z, 
                        2, 0.1, 0.2, 0.1, 0.02);
                }
                
                // Break soft blocks (dirt, gravel, sand)
                BlockPos checkPos = blockPos;
                Block block = world.getBlockState(checkPos).getBlock();
                if (block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.SAND || 
                    block == Blocks.COARSE_DIRT || block == Blocks.PODZOL || block == Blocks.MYCELIUM ||
                    block == Blocks.RED_SAND) {
                    world.breakBlock(checkPos, false);
                }
                
                // Check block above as well
                BlockPos abovePos = checkPos.up();
                Block aboveBlock = world.getBlockState(abovePos).getBlock();
                if (aboveBlock == Blocks.DIRT || aboveBlock == Blocks.GRAVEL || aboveBlock == Blocks.SAND ||
                    aboveBlock == Blocks.COARSE_DIRT || aboveBlock == Blocks.PODZOL || aboveBlock == Blocks.MYCELIUM ||
                    aboveBlock == Blocks.RED_SAND) {
                    world.breakBlock(abovePos, false);
                }
            }
            
            // Damage and apply slowness to entities in the line
            Vec3d lineStart = landingPos;
            Vec3d lineEnd = landingPos.add(slamDirection.multiply(SLAM_LINE_LENGTH));
            
            Box damageBox = new Box(
                Math.min(lineStart.x, lineEnd.x) - SLAM_WIDTH, 
                landingPos.y - 1, 
                Math.min(lineStart.z, lineEnd.z) - SLAM_WIDTH,
                Math.max(lineStart.x, lineEnd.x) + SLAM_WIDTH, 
                landingPos.y + 2, 
                Math.max(lineStart.z, lineEnd.z) + SLAM_WIDTH
            );
            
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, damageBox, e -> e != player)) {
                // Apply damage
                entity.damage(world.getDamageSources().playerAttack(player), SLAM_DAMAGE);
                
                // Apply slowness
                entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 
                    SLOWNESS_DURATION, 
                    1, // Amplifier 1 = Slowness II
                    false, 
                    true, 
                    true
                ));
            }
            });
        });
    }
}

