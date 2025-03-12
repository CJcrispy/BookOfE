package net.cjcrispy.entity.client.quinn;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.client.blackbird_warrior.BlackBirdModel;
import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.cjcrispy.entity.custom.ShadowQuinnEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShadowQuinnRenderer extends GeoEntityRenderer<ShadowQuinnEntity> {

    public ShadowQuinnRenderer(EntityRendererFactory.Context context) {
        super(context, new ShadowQuinnModel());
    }

    @Override
    public Identifier getTexture(ShadowQuinnEntity animatable) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/quinn/shadowquinn.png");
    }

    @Override
    public void render(ShadowQuinnEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
