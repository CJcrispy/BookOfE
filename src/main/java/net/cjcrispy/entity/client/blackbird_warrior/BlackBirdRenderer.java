package net.cjcrispy.entity.client.blackbird_warrior;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.client.dark_wizard.DarkWizardModel;
import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackBirdRenderer extends GeoEntityRenderer<BlackBirdEntity> {

    public BlackBirdRenderer(EntityRendererFactory.Context context) {
        super(context, new BlackBirdModel());
    }

    @Override
    public Identifier getTexture(BlackBirdEntity animatable) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/blackbird_warrior/blackbird_warrior.png");
    }

    @Override
    public void render(BlackBirdEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
