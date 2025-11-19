package net.cjcrispy.entity.client.slime_common;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeCommonEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SlimeCommonModel extends GeoModel<SlimeCommonEntity> {

	@Override
	public Identifier getModelResource(SlimeCommonEntity slimeCommonEntity) {
		return Identifier.of(BookOfE.MOD_ID, "geo/slime_common/slime_common.geo.json");
	}

	@Override
	public Identifier getTextureResource(SlimeCommonEntity slimeCommonEntity) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_common/slime_common.png");
	}

	@Override
	public Identifier getAnimationResource(SlimeCommonEntity slimeCommonEntity) {
		return Identifier.of(BookOfE.MOD_ID, "animations/slime_common/slime_common.animation.json");
	}
}

