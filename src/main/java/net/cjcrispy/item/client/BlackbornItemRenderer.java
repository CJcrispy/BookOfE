package net.cjcrispy.item.client;

import net.cjcrispy.BookOfE;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Custom renderer for Blackborn sword that handles animated texture rendering.
 * The animated texture is handled via .mcmeta file, but this renderer ensures
 * proper model rendering for the custom item model.
 */
public class BlackbornItemRenderer {
    /**
     * Renders the Blackborn item with animated texture support.
     * The animation is handled by Minecraft's built-in texture animation system via .mcmeta
     */
    public static void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers, int light, int overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        ItemRenderer itemRenderer = client.getItemRenderer();
        ModelIdentifier modelId = new ModelIdentifier(Identifier.of(BookOfE.MOD_ID, "the_black_blade"), "inventory");
        BakedModel model = client.getBakedModelManager().getModel(modelId);
        
        // Render the item with the model
        // The animated texture (#1 in the model) will be animated automatically via .mcmeta
        itemRenderer.renderItem(stack, mode, false, matrices, vertexConsumers, light, overlay, model);
    }
}

