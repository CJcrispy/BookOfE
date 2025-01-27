package net.cjcrispy.entity.client.dark_wizard;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.DarkWizardEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DarkWizardRenderer extends GeoEntityRenderer<DarkWizardEntity> {

    public DarkWizardRenderer(EntityRendererFactory.Context context) {
            super(context, new DarkWizardModel());
        }

    @Override
    public Identifier getTexture(DarkWizardEntity animatable) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/dark_wizard/l_darkwizard.png");
    }

    @Override
    public void render(DarkWizardEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
