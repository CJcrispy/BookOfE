package net.cjcrispy;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.client.blackbird_warrior.BlackBirdRenderer;
import net.cjcrispy.entity.client.dark_wizard.DarkWizardRenderer;
import net.cjcrispy.entity.client.quinn.ShadowQuinnRenderer;
import net.cjcrispy.entity.custom.*;
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

        EntityRendererRegistry.register(ModEntities.NICKY_SUMMONER, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(NickySummonerEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/nicky/nicky_mage.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.JOE_REBEL, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(JoeRebelEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/joe/joe_rebel.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.CHRIS_SLIME, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(SlimeChrisEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/chris/slime_chris.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.KING_HAJILE, context ->
                new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(KingHajileEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/eli/king_hajile.png");
                    }
                });
        EntityRendererRegistry.register(ModEntities.DARK_WIZARD, DarkWizardRenderer::new);

        EntityRendererRegistry.register(ModEntities.BLACKBIRD_WARRIOR, BlackBirdRenderer::new);

        EntityRendererRegistry.register(ModEntities.SHADOW_QUINN, ShadowQuinnRenderer::new);
    }
}