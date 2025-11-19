package net.cjcrispy.procedure.chris;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.SlimeChrisEntity;
import net.cjcrispy.entity.custom.SlimeCommonEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class SummonCustomSlime {
    public static void execute(SlimeChrisEntity mob) {
        if (mob == null || mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();

        // Summoning particles
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 1.5;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1.0;
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.ITEM_SLIME, x, y, z, 1, 0, 0.1, 0, 0.05);
        }

        // Play summon sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH,
                mob.getSoundCategory(), 1.0F, 0.8F);

        // Delay the actual summoning
        world.getServer().execute(() -> {
            // Summon 1-2 custom slimes around Chris
            int summonCount = 1 + mob.getRandom().nextInt(2); // 1 or 2 summons

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

                // Create and spawn custom slime with scale 3
                SlimeCommonEntity summoned = ModEntities.SLIME_COMMON.create(world);
                if (summoned != null) {
                    summoned.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                            mob.getRandom().nextFloat() * 360.0F, 0.0F);

                    // Set scale to 3
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_SCALE)
                            .setBaseValue(3.0);

                    // Boost stats for boss fight slimes (only when summoned by boss)
                    // Health: 1.0 -> 20.0 (much more durable)
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH)
                            .setBaseValue(20.0);
                    summoned.setHealth(20.0f); // Set current health to max
                    
                    // Attack Damage: 2.0 -> 6.0 (more dangerous)
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)
                            .setBaseValue(6.0);
                    
                    // Movement Speed: 0.25 -> 0.30 (slightly faster)
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(0.30);
                    
                    // Follow Range: 16.0 -> 24.0 (better tracking)
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE)
                            .setBaseValue(24.0);
                    
                    // Add armor for extra durability
                    summoned.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR)
                            .setBaseValue(4.0);

                    // Set owner to prevent friendly fire
                    summoned.setOwnerUuid(mob.getUuid());

                    // Set target to an attacker of the boss (if any exist)
                    // The slime will only target entities that have attacked the boss
                    java.util.Set<java.util.UUID> attackers = mob.getAttackers();
                    if (!attackers.isEmpty()) {
                        // Find the closest attacker and set as target
                        LivingEntity closestAttacker = null;
                        double closestDistance = Double.MAX_VALUE;
                        net.minecraft.util.math.Box searchBox = summoned.getBoundingBox().expand(50.0);
                        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, searchBox, 
                                e -> e.isAlive() && attackers.contains(e.getUuid()))) {
                            double distance = summoned.squaredDistanceTo(entity);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestAttacker = entity;
                            }
                        }
                        if (closestAttacker != null) {
                            summoned.setTarget(closestAttacker);
                        }
                    }

                    world.spawnEntity(summoned);

                    // Summon particles at spawn location
                    for (int j = 0; j < 10; j++) {
                        world.spawnParticles(ParticleTypes.ITEM_SLIME,
                                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                                3, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }

            // Play summon completion sound
            world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH,
                    mob.getSoundCategory(), 1.0F, 0.6F);
        });
    }
}

