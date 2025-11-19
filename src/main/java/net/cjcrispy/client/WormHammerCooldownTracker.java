package net.cjcrispy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WormHammerCooldownTracker {
    private static final Map<UUID, Integer> clientCooldowns = new HashMap<>();
    private static final int MAX_COOLDOWN = 240; // 12 seconds (matches WormHammerItem.BURROW_SLAM_COOLDOWN_TICKS)
    
    public static void tick() {
        if (MinecraftClient.getInstance().player == null) return;
        
        PlayerEntity player = MinecraftClient.getInstance().player;
        UUID playerId = player.getUuid();
        
        // Always tick cooldown regardless of whether weapon is in hand
        // This ensures the cooldown persists when switching items
        int cooldown = clientCooldowns.getOrDefault(playerId, 0);
        if (cooldown > 0) {
            clientCooldowns.put(playerId, cooldown - 1);
        } else {
            clientCooldowns.remove(playerId);
        }
    }
    
    public static float getCooldownProgress(UUID playerId) {
        int cooldown = clientCooldowns.getOrDefault(playerId, 0);
        if (cooldown <= 0) {
            return 1.0f; // Fully charged
        }
        return 1.0f - ((float) cooldown / MAX_COOLDOWN);
    }
    
    public static float getOpacity(UUID playerId) {
        float progress = getCooldownProgress(playerId);
        // When on cooldown (progress = 0), dim to 0.3 opacity
        // When ready (progress = 1.0), full opacity (1.0)
        // This creates a smooth fade from dim to bright as cooldown progresses
        return 0.3f + (progress * 0.7f);
    }
    
    public static boolean isOnCooldown(UUID playerId) {
        return clientCooldowns.getOrDefault(playerId, 0) > 0;
    }
    
    public static void setCooldown(UUID playerId, int ticks) {
        if (ticks <= 0) {
            clientCooldowns.remove(playerId);
        } else {
            clientCooldowns.put(playerId, ticks);
        }
    }
    
    public static void reset() {
        clientCooldowns.clear();
    }
}

