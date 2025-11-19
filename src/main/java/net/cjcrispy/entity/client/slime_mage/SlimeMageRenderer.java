package net.cjcrispy.entity.client.slime_mage;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeMageEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SlimeMageRenderer extends GeoEntityRenderer<SlimeMageEntity> {

	public SlimeMageRenderer(EntityRendererFactory.Context context) {
		super(context, new SlimeMageModel());
	}

	@Override
	public Identifier getTexture(SlimeMageEntity animatable) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_mage/slime_mage.png");
	}

	@Override
	public void render(SlimeMageEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
					   VertexConsumerProvider bufferSource, int packedLight) {
		poseStack.scale(0.6f, 0.6f, 0.6f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}
}

