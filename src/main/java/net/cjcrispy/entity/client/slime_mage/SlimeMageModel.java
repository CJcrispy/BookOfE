package net.cjcrispy.entity.client.slime_mage;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeMageEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SlimeMageModel extends GeoModel<SlimeMageEntity> {

	@Override
	public Identifier getModelResource(SlimeMageEntity slimeMageEntity) {
		return Identifier.of(BookOfE.MOD_ID, "geo/slime_mage/slime_mage.geo.json");
	}

	@Override
	public Identifier getTextureResource(SlimeMageEntity slimeMageEntity) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_mage/slime_mage.png");
	}

	@Override
	public Identifier getAnimationResource(SlimeMageEntity slimeMageEntity) {
		return Identifier.of(BookOfE.MOD_ID, "animations/slime_common/slime_common.animation.json");
	}
}

