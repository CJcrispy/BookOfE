package net.cjcrispy.entity.client.eli;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.KingHajileEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class KingHajileRenderer extends LivingEntityRenderer<KingHajileEntity, PlayerEntityModel<KingHajileEntity>> {

    public KingHajileRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<KingHajileEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Override
    public Identifier getTexture(KingHajileEntity entity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/eli/king_hajile.png");
    }
}
