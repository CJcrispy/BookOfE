package net.cjcrispy.entity.client;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;


public class MillyKnightRenderer extends LivingEntityRenderer<MillyKnightEntity, PlayerEntityModel<MillyKnightEntity>> {

    public MillyKnightRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }


    @Override
    public Identifier getTexture(MillyKnightEntity entity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/milly/milly_knight.png");
    }
}



