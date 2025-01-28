package net.cjcrispy;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.client.dark_wizard.DarkWizardRenderer;
import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.entity.custom.QuinnKnightEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class BookOfEClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ModEntities.MILLY_KNIGHT, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(MillyKnightEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/milly/milly_knight.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.QUINN_KNIGHT, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(QuinnKnightEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/quinn/quinn_knight.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.DARK_WIZARD, DarkWizardRenderer::new);
    }
}