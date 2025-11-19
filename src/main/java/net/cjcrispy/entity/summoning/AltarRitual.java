package net.cjcrispy.entity.summoning;

import net.cjcrispy.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the ritual animation and delayed summoning for the Cult Altar.
 */
public final class AltarRitual {
    
    private static final int RITUAL_DURATION_TICKS = 120; // 6 seconds
    private static final double PARTICLE_RADIUS = 1.5D;
    private static final double PARTICLE_HEIGHT = 0.5D;
    
    private static final Map<BlockPos, RitualData> activeRituals = new HashMap<>();
    
    private static class RitualData {
        final ServerWorld world;
        final Item keyItem;
        final BlockPos spawnPos;
        final long startTime;
        
        RitualData(ServerWorld world, Item keyItem, BlockPos spawnPos, long startTime) {
            this.world = world;
            this.keyItem = keyItem;
            this.spawnPos = spawnPos;
            this.startTime = startTime;
        }
    }
    
    private AltarRitual() {
    }
    
    /**
     * Starts a ritual at the altar position.
     * Spawns particles for 6 seconds, then summons the entity and destroys the altar.
     */
    public static void startRitual(ServerWorld world, BlockPos altarPos, Item keyItem, BlockPos spawnPos) {
        net.cjcrispy.BookOfE.LOGGER.info("Starting altar ritual at {} with key {}", altarPos, keyItem);
        
        // Play ritual start sound
        world.playSound(null, altarPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 0.8F);
        
        // Register the ritual
        activeRituals.put(altarPos, new RitualData(world, keyItem, spawnPos, world.getTime()));
    }
    
    /**
     * Called every world tick to update active rituals.
     * Should be called from a world tick event.
     */
    public static void tickRituals(ServerWorld world) {
        long currentTime = world.getTime();
        activeRituals.entrySet().removeIf(entry -> {
            BlockPos altarPos = entry.getKey();
            RitualData ritual = entry.getValue();
            
            if (ritual.world != world) {
                return false; // Don't remove rituals from other worlds
            }
            
            long elapsedTicks = currentTime - ritual.startTime;
            
            if (elapsedTicks >= RITUAL_DURATION_TICKS) {
                // Ritual complete
                completeRitual(world, altarPos, ritual.keyItem, ritual.spawnPos);
                return true; // Remove from map
            }
            
            // Spawn particles
            double centerX = altarPos.getX() + 0.5D;
            double centerY = altarPos.getY() + PARTICLE_HEIGHT;
            double centerZ = altarPos.getZ() + 0.5D;
            
            // Check for special particle effects based on key item
            boolean isNickyKey = ritual.keyItem == ModItems.NICKY_SUMMONING_KEY;
            boolean isMillyKey = ritual.keyItem == ModItems.MILLY_KEY;
            boolean isEliKey = ritual.keyItem == ModItems.ELI_SUMMONING_KEY;
            boolean isJoeKey = ritual.keyItem == ModItems.JOE_KEY;
            boolean isCryingObsidian = ritual.keyItem == net.minecraft.item.Items.CRYING_OBSIDIAN;
            boolean isSlimeBlock = ritual.keyItem == net.minecraft.item.Items.SLIME_BLOCK;
            
            if (isNickyKey) {
                // Blue chaotic particles for Nicky
                int particlesPerTick = 12; // More particles for chaotic effect
                for (int i = 0; i < particlesPerTick; i++) {
                    // Chaotic movement - random angles and positions
                    double angle = (elapsedTicks * 0.15D) + (i * (Math.PI * 2 / particlesPerTick)) + (world.random.nextDouble() * 0.5D);
                    double radius = PARTICLE_RADIUS + (world.random.nextDouble() * 0.5D - 0.25D);
                    double x = centerX + MathHelper.cos((float) angle) * radius;
                    double z = centerZ + MathHelper.sin((float) angle) * radius;
                    double y = centerY + (world.random.nextDouble() * 0.5D - 0.25D);
                    
                    // Blue dust particles (RGB: 0.0, 0.0, 1.0 for blue)
                    world.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.0f, 0.0f, 1.0f), 1.0f),
                        x, y, z,
                        1, 0.0, 0.0, 0.0, 0.1
                    );
                }
                
                // Additional chaotic blue particles rising up
                if (elapsedTicks % 3 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.0f, 0.0f, 1.0f), 1.0f),
                            centerX + offsetX, centerY, centerZ + offsetZ,
                            1, 0.0, 0.3, 0.0, 0.15
                        );
                    }
                }
            } else if (isMillyKey) {
                // Red particles for Milly Knight
                int particlesPerTick = 10;
                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = (elapsedTicks * 0.12D) + (i * (Math.PI * 2 / particlesPerTick));
                    double radius = PARTICLE_RADIUS + (world.random.nextDouble() * 0.3D - 0.15D);
                    double x = centerX + MathHelper.cos((float) angle) * radius;
                    double z = centerZ + MathHelper.sin((float) angle) * radius;
                    double y = centerY + (world.random.nextDouble() * 0.4D - 0.2D);
                    
                    // Red dust particles (RGB: 1.0, 0.0, 0.0 for red)
                    world.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                        x, y, z,
                        1, 0.0, 0.0, 0.0, 0.1
                    );
                }
                
                // Additional red particles rising up
                if (elapsedTicks % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 0.6;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 0.6;
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                            centerX + offsetX, centerY, centerZ + offsetZ,
                            1, 0.0, 0.25, 0.0, 0.12
                        );
                    }
                }
            } else if (isCryingObsidian) {
                // Purple particles for Shadow Quinn (Crying Obsidian)
                int particlesPerTick = 10;
                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = (elapsedTicks * 0.12D) + (i * (Math.PI * 2 / particlesPerTick));
                    double radius = PARTICLE_RADIUS + (world.random.nextDouble() * 0.3D - 0.15D);
                    double x = centerX + MathHelper.cos((float) angle) * radius;
                    double z = centerZ + MathHelper.sin((float) angle) * radius;
                    double y = centerY + (world.random.nextDouble() * 0.4D - 0.2D);
                    
                    // Purple dust particles (RGB: 0.5, 0.0, 1.0 for purple)
                    world.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.5f, 0.0f, 1.0f), 1.0f),
                        x, y, z,
                        1, 0.0, 0.0, 0.0, 0.1
                    );
                }
                
                // Additional purple particles rising up
                if (elapsedTicks % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 0.6;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 0.6;
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.5f, 0.0f, 1.0f), 1.0f),
                            centerX + offsetX, centerY, centerZ + offsetZ,
                            1, 0.0, 0.25, 0.0, 0.12
                        );
                    }
                }
            } else if (isSlimeBlock) {
                // Green particles for Slime Chris (Slime Block)
                int particlesPerTick = 9;
                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = (elapsedTicks * 0.11D) + (i * (Math.PI * 2 / particlesPerTick));
                    double radius = PARTICLE_RADIUS + (world.random.nextDouble() * 0.2D - 0.1D);
                    double x = centerX + MathHelper.cos((float) angle) * radius;
                    double z = centerZ + MathHelper.sin((float) angle) * radius;
                    double y = centerY + (world.random.nextDouble() * 0.3D - 0.15D);
                    
                    // Green dust particles (RGB: 0.0, 1.0, 0.0 for green)
                    world.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.0f, 1.0f, 0.0f), 1.0f),
                        x, y, z,
                        1, 0.0, 0.0, 0.0, 0.1
                    );
                }
                
                // Additional green particles rising up
                if (elapsedTicks % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 0.6;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 0.6;
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.0f, 1.0f, 0.0f), 1.0f),
                            centerX + offsetX, centerY, centerZ + offsetZ,
                            1, 0.0, 0.25, 0.0, 0.12
                        );
                    }
                }
            } else if (isEliKey) {
                // Yellow holy beam particles for Eli - dramatic beam from the sky
                // Main beam column - particles falling from above
                double beamHeight = 20.0D; // Height of the beam
                int beamParticles = 15; // Particles in the beam column
                for (int i = 0; i < beamParticles; i++) {
                    double progress = (double) i / beamParticles; // 0.0 to 1.0
                    double y = centerY + beamHeight * (1.0D - progress); // From top to bottom
                    double offsetX = (world.random.nextDouble() - 0.5) * 0.3; // Small random offset
                    double offsetZ = (world.random.nextDouble() - 0.5) * 0.3;
                    
                    // Yellow/golden particles (RGB: 1.0, 1.0, 0.0 for yellow, slightly golden)
                    world.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.9f, 0.0f), 1.2f),
                        centerX + offsetX, y, centerZ + offsetZ,
                        1, 0.0, -0.1, 0.0, 0.08
                    );
                }
                
                // Additional bright particles at the altar
                if (elapsedTicks % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = i * (Math.PI * 2 / 8.0);
                        double radius = 0.5D + (world.random.nextDouble() * 0.3D);
                        double x = centerX + MathHelper.cos((float) angle) * radius;
                        double z = centerZ + MathHelper.sin((float) angle) * radius;
                        double y = centerY + (world.random.nextDouble() * 0.5D);
                        
                        // Bright yellow particles
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(1.0f, 1.0f, 0.0f), 1.5f),
                            x, y, z,
                            1, 0.0, 0.0, 0.0, 0.12
                        );
                    }
                }
                
                // Particles radiating outward from the beam
                if (elapsedTicks % 3 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (elapsedTicks * 0.1D) + (i * (Math.PI * 2 / 12.0));
                        double radius = 1.0D + (world.random.nextDouble() * 0.5D);
                        double x = centerX + MathHelper.cos((float) angle) * radius;
                        double z = centerZ + MathHelper.sin((float) angle) * radius;
                        double y = centerY + (world.random.nextDouble() * 0.3D);
                        
                        world.spawnParticles(
                            new DustParticleEffect(new Vector3f(1.0f, 0.95f, 0.2f), 1.0f),
                            x, y, z,
                            1, 0.0, 0.0, 0.0, 0.1
                        );
                    }
                }
            } else if (isJoeKey) {
                // Black particles for Joe
                int particlesPerTick = 8;
                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = (elapsedTicks * 0.1D) + (i * (Math.PI * 2 / particlesPerTick));
                    double x = centerX + MathHelper.cos((float) angle) * PARTICLE_RADIUS;
                    double z = centerZ + MathHelper.sin((float) angle) * PARTICLE_RADIUS;
                    
                    // Use black particles (SMOKE)
                    world.spawnParticles(
                        ParticleTypes.SMOKE,
                        x, centerY, z,
                        1, 0.0, 0.0, 0.0, 0.02
                    );
                }
                
                // Also spawn some particles rising up
                if (elapsedTicks % 5 == 0) {
                    world.spawnParticles(
                        ParticleTypes.SMOKE,
                        centerX, centerY, centerZ,
                        3, 0.3, 0.5, 0.3, 0.05
                    );
                }
            } else {
                // Default black particles for other keys
                int particlesPerTick = 8;
                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = (elapsedTicks * 0.1D) + (i * (Math.PI * 2 / particlesPerTick));
                    double x = centerX + MathHelper.cos((float) angle) * PARTICLE_RADIUS;
                    double z = centerZ + MathHelper.sin((float) angle) * PARTICLE_RADIUS;
                    
                    // Use black particles (SMOKE)
                    world.spawnParticles(
                        ParticleTypes.SMOKE,
                        x, centerY, z,
                        1, 0.0, 0.0, 0.0, 0.02
                    );
                }
                
                // Also spawn some particles rising up
                if (elapsedTicks % 5 == 0) {
                    world.spawnParticles(
                        ParticleTypes.SMOKE,
                        centerX, centerY, centerZ,
                        3, 0.3, 0.5, 0.3, 0.05
                    );
                }
            }
            
            return false; // Keep in map
        });
    }
    
    private static void completeRitual(ServerWorld world, BlockPos altarPos, Item keyItem, BlockPos spawnPos) {
        net.cjcrispy.BookOfE.LOGGER.info("Completing ritual - summoning entity and destroying altar");
        
        // Play completion sound
        world.playSound(null, altarPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
        
        // Spawn final explosion of particles
        double centerX = altarPos.getX() + 0.5D;
        double centerY = altarPos.getY() + 0.5D;
        double centerZ = altarPos.getZ() + 0.5D;
        
        boolean isNickyKey = keyItem == ModItems.NICKY_SUMMONING_KEY;
        boolean isMillyKey = keyItem == ModItems.MILLY_KEY;
        boolean isEliKey = keyItem == ModItems.ELI_SUMMONING_KEY;
        boolean isJoeKey = keyItem == ModItems.JOE_KEY;
        boolean isCryingObsidian = keyItem == net.minecraft.item.Items.CRYING_OBSIDIAN;
        boolean isSlimeBlock = keyItem == net.minecraft.item.Items.SLIME_BLOCK;
        
        for (int i = 0; i < 30; i++) {
            double angle = i * (Math.PI * 2 / 30.0);
            double x = centerX + MathHelper.cos((float) angle) * 2.0;
            double z = centerZ + MathHelper.sin((float) angle) * 2.0;
            
            if (isNickyKey) {
                // Blue explosion particles
                world.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 0.0f, 1.0f), 1.0f),
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.15
                );
            } else if (isMillyKey) {
                // Red explosion particles
                world.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.15
                );
            } else if (isCryingObsidian) {
                // Purple explosion particles
                world.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.5f, 0.0f, 1.0f), 1.0f),
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.15
                );
            } else if (isSlimeBlock) {
                // Green explosion particles
                world.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 1.0f, 0.0f), 1.0f),
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.15
                );
            } else if (isEliKey) {
                // Yellow/golden explosion particles
                world.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.9f, 0.0f), 1.2f),
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.15
                );
            } else if (isJoeKey) {
                // Black smoke particles
                world.spawnParticles(
                    ParticleTypes.SMOKE,
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.1
                );
            } else {
                // Black smoke particles
                world.spawnParticles(
                    ParticleTypes.SMOKE,
                    x, centerY, z,
                    2, 0.0, 0.3, 0.0, 0.1
                );
            }
        }
        
        // Summon the entity
        net.minecraft.entity.Entity summoned = BossSummoningRegistry.trySummon(keyItem, world, spawnPos);
        
        if (summoned != null) {
            net.cjcrispy.BookOfE.LOGGER.info("Successfully summoned {} at {}", summoned.getType(), spawnPos);
        } else {
            net.cjcrispy.BookOfE.LOGGER.error("Failed to summon entity with key {}", keyItem);
        }
        
        // Destroy the altar
        world.breakBlock(altarPos, false);
    }
}

