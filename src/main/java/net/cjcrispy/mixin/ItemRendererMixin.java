package net.cjcrispy.mixin;

import net.cjcrispy.client.WormHammerCooldownTracker;
import net.cjcrispy.item.custom.WormHammerItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InGameHud.class)
public class ItemRendererMixin {
    @Shadow @Final private MinecraftClient client;
    
    @Inject(
        method = "renderHotbarItem",
        at = @At("TAIL")
    )
    private void onRenderHotbarItem(DrawContext context, int x, int y, net.minecraft.client.render.RenderTickCounter tickCounter, net.minecraft.entity.player.PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (this.client.player != null && (stack.getItem() instanceof WormHammerItem)) {
            // Use the client player's UUID to match what we're tracking
            UUID playerId = this.client.player.getUuid();
            
            // Check if the item is on cooldown (even if not currently in hand, check inventory)
            if (WormHammerCooldownTracker.isOnCooldown(playerId)) {
                float opacity = WormHammerCooldownTracker.getOpacity(playerId);
                
                // Apply opacity by rendering a dimming overlay
                // opacity ranges from 0.3 (fully on cooldown) to 1.0 (ready)
                if (opacity < 0.99f) { // Use 0.99f to account for floating point precision
                    // Calculate the dimming color (black overlay with variable alpha)
                    // When opacity is 0.3 (on cooldown), we want strong dimming
                    // When opacity is 1.0 (ready), no dimming
                    // This creates a smooth fade from dim to bright as cooldown progresses
                    float dimming = 1.0f - opacity; // 0.7 when on cooldown, 0.0 when ready
                    int alpha = (int) (dimming * 255); // Use full alpha range for better visibility
                    int color = (alpha << 24) | 0x000000; // Black with variable alpha (ARGB format: AARRGGBB)
                    
                    // Render dimming overlay on top of the item
                    // Use a higher z-level to ensure it renders on top
                    MatrixStack matrices = context.getMatrices();
                    matrices.push();
                    matrices.translate(0, 0, 300); // Higher z-level to render on top
                    context.fill(x, y, x + 16, y + 16, color);
                    matrices.pop();
                }
            }
        }
    }
}

