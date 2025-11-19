package net.cjcrispy.entity.client.slime_warrior;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeWarriorEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SlimeWarriorRenderer extends GeoEntityRenderer<SlimeWarriorEntity> {

	public SlimeWarriorRenderer(EntityRendererFactory.Context context) {
		super(context, new SlimeWarriorModel());
	}

	@Override
	public Identifier getTexture(SlimeWarriorEntity animatable) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_warrior/slime_warrior.png");
	}

	@Override
	public void render(SlimeWarriorEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
					   VertexConsumerProvider bufferSource, int packedLight) {
		poseStack.scale(0.6f, 0.6f, 0.6f);
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}
}

