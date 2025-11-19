package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.procedure.wormhammer.BurrowSlam;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WormHammerItem extends SwordItem {
    private static final int BURROW_SLAM_COOLDOWN_TICKS = WeaponConfig.WormHammer.BURROW_SLAM_COOLDOWN_TICKS; // 12 seconds (12 * 20 ticks)
    
    // Track hit counts and cooldowns per player
    private static final Map<UUID, Integer> hitCounts = new HashMap<>();
    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    
    public WormHammerItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        UUID playerId = user.getUuid();
        
        // Check cooldown
        int cooldown = getCooldown(playerId);
        if (cooldown > 0) {
            return TypedActionResult.fail(stack);
        }

		// Apply vanilla item cooldown for visual feedback (hotbar overlay)
		user.getItemCooldownManager().set(this, BURROW_SLAM_COOLDOWN_TICKS);
        
        if (world.isClient()) {
            // On client, set cooldown for visual feedback
            net.cjcrispy.client.WormHammerCooldownTracker.setCooldown(playerId, BURROW_SLAM_COOLDOWN_TICKS);
            return TypedActionResult.success(stack);
        }
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Start charging
        user.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 0.5f, 1.2f);
        
        // Set cooldown immediately
        setCooldown(playerId, BURROW_SLAM_COOLDOWN_TICKS);
        
        // Execute Burrow Slam after 1 second (20 ticks)
        serverWorld.getServer().execute(() -> {
            BurrowSlam.execute(user, serverWorld);
        });
        
        return TypedActionResult.success(stack, false);
    }
    
    public static int getHitCount(UUID playerId) {
        return hitCounts.getOrDefault(playerId, 0);
    }
    
    public static void setHitCount(UUID playerId, int count) {
        if (count <= 0) {
            hitCounts.remove(playerId);
        } else {
            hitCounts.put(playerId, count);
        }
    }
    
    public static void incrementHitCount(UUID playerId) {
        setHitCount(playerId, getHitCount(playerId) + 1);
    }
    
    public static int getCooldown(UUID playerId) {
        return cooldowns.getOrDefault(playerId, 0);
    }
    
    public static void setCooldown(UUID playerId, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(playerId);
        } else {
            cooldowns.put(playerId, ticks);
        }
    }
    
    public static void tickCooldown(UUID playerId) {
        int cooldown = getCooldown(playerId);
        if (cooldown > 0) {
            setCooldown(playerId, cooldown - 1);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.bookofe.worm_hammer.passive").formatted(Formatting.DARK_AQUA));
        tooltip.add(Text.translatable("item.bookofe.worm_hammer.passive.desc").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.bookofe.worm_hammer.ability").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.bookofe.worm_hammer.ability.desc").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

