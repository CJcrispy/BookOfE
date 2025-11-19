package net.cjcrispy;

import net.cjcrispy.client.WormHammerCooldownTracker;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.client.blackbird_warrior.BlackBirdRenderer;
import net.cjcrispy.entity.client.dark_wizard.DarkWizardRenderer;
import net.cjcrispy.entity.client.quinn.ShadowQuinnRenderer;
import net.cjcrispy.entity.client.slime_common.SlimeCommonRenderer;
import net.cjcrispy.entity.client.slime_mage.SlimeMageRenderer;
import net.cjcrispy.entity.client.slime_warrior.SlimeWarriorRenderer;
import net.cjcrispy.entity.custom.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BookOfEClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        
        // Register client tick handlers for cooldown tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WormHammerCooldownTracker.tick();
        });

        EntityRendererRegistry.register(ModEntities.MILLY_KNIGHT, context ->
                new MobEntityRenderer<MillyKnightEntity, PlayerEntityModel<MillyKnightEntity>>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    {
                        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
                    }

                    @Override
                    protected void scale(MillyKnightEntity mob, MatrixStack matrices, float tickDelta) {
                        float scale = 1.35F;
                        matrices.scale(scale, scale, scale);
                    }

                    @Override
                    public Identifier getTexture(MillyKnightEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/milly/milly_knight.png");
                    }
                });


        EntityRendererRegistry.register(ModEntities.NICKY_SUMMONER, context ->
                new MobEntityRenderer<NickySummonerEntity, PlayerEntityModel<NickySummonerEntity>>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(NickySummonerEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/nicky/nicky_mage.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.JOE_REBEL, context ->
                new MobEntityRenderer<JoeRebelEntity, PlayerEntityModel<JoeRebelEntity>>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(JoeRebelEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/joe/joe_rebel.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.CHRIS_SLIME, context ->
                new MobEntityRenderer<SlimeChrisEntity, PlayerEntityModel<SlimeChrisEntity>>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(SlimeChrisEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/chris/slime_chris.png");
                    }
                });

        EntityRendererRegistry.register(ModEntities.KING_HAJILE, context ->
                new MobEntityRenderer<KingHajileEntity, PlayerEntityModel<KingHajileEntity>>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F) {
                    @Override
                    public Identifier getTexture(KingHajileEntity entity) {
                        return Identifier.of(BookOfE.MOD_ID, "textures/entity/eli/king_hajile.png");
                    }
                });
        EntityRendererRegistry.register(ModEntities.DARK_WIZARD, DarkWizardRenderer::new);

        EntityRendererRegistry.register(ModEntities.BLACKBIRD_WARRIOR, BlackBirdRenderer::new);

        EntityRendererRegistry.register(ModEntities.SHADOW_QUINN, ShadowQuinnRenderer::new);
        
        EntityRendererRegistry.register(ModEntities.SLIME_COMMON, SlimeCommonRenderer::new);
        EntityRendererRegistry.register(ModEntities.SLIME_MAGE, SlimeMageRenderer::new);
        EntityRendererRegistry.register(ModEntities.SLIME_WARRIOR, SlimeWarriorRenderer::new);
    }
}