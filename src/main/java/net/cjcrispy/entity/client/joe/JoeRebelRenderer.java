package net.cjcrispy.entity.client.joe;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.JoeRebelEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class JoeRebelRenderer extends LivingEntityRenderer<JoeRebelEntity, PlayerEntityModel<JoeRebelEntity>> {
    public JoeRebelRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<JoeRebelEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Override
    public Identifier getTexture(JoeRebelEntity entity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/joe/joe_rebel.png");
    }
}
