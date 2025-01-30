package net.cjcrispy.procedure.quinn;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class QuinnKnightMelee {
    public static void execute(Entity entity) {
        if (entity == null) return;

        if (entity instanceof LivingEntity livingEntity) {
            // Create a new Iron Sword ItemStack
            ItemStack swordStack = new ItemStack(Items.IRON_SWORD);

            // Equip the sword in the main hand
            livingEntity.setStackInHand(Hand.MAIN_HAND, swordStack);

            // If the entity is a player, update the inventory
            if (livingEntity instanceof PlayerEntity player) {
                player.getInventory().markDirty(); // Use markDirty to signal that the inventory has changed
            }
        }
    }
}