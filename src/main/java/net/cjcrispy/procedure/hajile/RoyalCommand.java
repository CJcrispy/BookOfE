package net.cjcrispy.procedure.hajile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

public class RoyalCommand {
    private static final int SUMMON_COUNT = 3;
    
    public static void execute(Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;
        if (mob.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Stop movement
        mob.getNavigation().stop();
        
        // Summon particles
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2;
            double radius = 2.0;
            double x = mob.getX() + Math.cos(angle) * radius;
            double y = mob.getY() + 0.5;
            double z = mob.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 3, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.15, 0.15, 0.15, 0.05);
        }
        
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 
                mob.getSoundCategory(), 1.0F, 0.8F);
        
        // Wait 10 ticks before summoning
        world.getServer().execute(() -> {
            summonKnights(world, mob, 0);
        });
    }
    
    private static void summonKnights(ServerWorld world, MobEntity mob, int count) {
        if (count >= SUMMON_COUNT || !mob.isAlive()) {
            return;
        }
        
        // Summon position (around Hajile)
        double angle = (count / (double) SUMMON_COUNT) * Math.PI * 2;
        double radius = 3.0;
        double x = mob.getX() + Math.cos(angle) * radius;
        double y = mob.getY();
        double z = mob.getZ() + Math.sin(angle) * radius;
        
        // Create Holy Knight (using SkeletonEntity as base, can be replaced with custom entity)
        SkeletonEntity knight = EntityType.SKELETON.create(world);
        if (knight != null) {
            knight.refreshPositionAndAngles(x, y, z, (float) (angle * 180 / Math.PI), 0);
            knight.setCustomName(net.minecraft.text.Text.literal("Holy Knight"));
            knight.setHealth(20.0f); // Low HP
            
            // Equip with golden sword and shield
            knight.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            knight.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            
            // Make them glow softly
            knight.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
            
            // Set target to Hajile's target
            if (mob.getTarget() != null) {
                knight.setTarget(mob.getTarget());
            }
            
            // Summon effect
            for (int i = 0; i < 20; i++) {
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1, z, 5, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.FIREWORK, x, y + 1, z, 3, 0.3, 0.3, 0.3, 0.05);
            }
            
            world.spawnEntity(knight);
        }
        
        // Summon next knight after delay
        world.getServer().execute(() -> {
            summonKnights(world, mob, count + 1);
        });
    }
}

