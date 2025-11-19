package net.cjcrispy.entity.client.slime_common;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeCommonEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SlimeCommonRenderer extends GeoEntityRenderer<SlimeCommonEntity> {

	public SlimeCommonRenderer(EntityRendererFactory.Context context) {
		super(context, new SlimeCommonModel());
	}

	@Override
	public Identifier getTexture(SlimeCommonEntity animatable) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_common/slime_common.png");
	}

	@Override
	public void render(SlimeCommonEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
					   VertexConsumerProvider bufferSource, int packedLight) {
		poseStack.scale(0.6f, 0.6f, 0.6f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}
}

