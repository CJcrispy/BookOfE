package net.cjcrispy.procedure.nicky;

import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.List;

public class NickySummonerCommand {
    private static final double COMMAND_RANGE = 30.0; // 30 block range
    private static final int BUFF_DURATION_TICKS = 200; // 10 seconds (20 ticks per second)
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob) || mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Make Nicky glow with animation
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false, false));
        mob.getWorld().sendEntityStatus(mob, (byte) 4); // Casting animation
        
        // Play command sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 
                mob.getSoundCategory(), 1.2F, 0.8F);
        
        // Command particles around Nicky
        for (int i = 0; i < 40; i++) {
            double angle = (i / 40.0) * Math.PI * 2;
            double radius = 1.0 + (i % 5) * 0.3;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 1.0 + (i % 3) * 0.2;
            double z = mob.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
        }
        
        // Find all BlackBird entities within range
        Box searchBox = mob.getBoundingBox().expand(COMMAND_RANGE);
        List<BlackBirdEntity> minions = world.getEntitiesByClass(BlackBirdEntity.class, searchBox, 
                minion -> minion.isAlive() && mob.squaredDistanceTo(minion) <= COMMAND_RANGE * COMMAND_RANGE);
        
        if (minions.isEmpty()) {
            return; // No minions to buff
        }
        
        // Buff all minions
        for (BlackBirdEntity minion : minions) {
            // Apply Speed II and Strength I
            minion.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BUFF_DURATION_TICKS, 1, false, false, true));
            minion.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BUFF_DURATION_TICKS, 0, false, false, true));
            
            // Some minions get regeneration (30% chance)
            if (mob.getRandom().nextFloat() < 0.3f) {
                minion.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, BUFF_DURATION_TICKS, 0, false, false, true));
            }
            
            // Some minions get explosion on death (20% chance)
            if (mob.getRandom().nextFloat() < 0.2f) {
                minion.setShouldExplodeOnDeath(true);
            }
            
            // Visual effect on minion - glow for entire buff duration
            minion.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, BUFF_DURATION_TICKS, 0, false, false, false));
            
            // Particles around the minion
            for (int i = 0; i < 10; i++) {
                double angle = (i / 10.0) * Math.PI * 2;
                double radius = 0.5;
                double x = minion.getX() + Math.cos(angle) * radius;
                double y = minion.getY() + 0.5;
                double z = minion.getZ() + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
            }
        }
        
        // Play completion sound
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 
                mob.getSoundCategory(), 1.0F, 1.2F);
    }
}

