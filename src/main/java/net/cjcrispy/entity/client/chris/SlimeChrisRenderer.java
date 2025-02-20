package net.cjcrispy.entity.client.chris;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.KingHajileEntity;
import net.cjcrispy.entity.custom.SlimeChrisEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class SlimeChrisRenderer extends LivingEntityRenderer<SlimeChrisEntity, PlayerEntityModel<SlimeChrisEntity>> {
    public SlimeChrisRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<SlimeChrisEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Override
    public Identifier getTexture(SlimeChrisEntity entity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/chris/slime_chris.png");
    }
}
