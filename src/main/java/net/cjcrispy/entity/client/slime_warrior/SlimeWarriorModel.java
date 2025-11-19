package net.cjcrispy.entity.client.slime_warrior;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.SlimeWarriorEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SlimeWarriorModel extends GeoModel<SlimeWarriorEntity> {

	@Override
	public Identifier getModelResource(SlimeWarriorEntity slimeWarriorEntity) {
		return Identifier.of(BookOfE.MOD_ID, "geo/slime_warrior/slime_warrior.geo.json");
	}

	@Override
	public Identifier getTextureResource(SlimeWarriorEntity slimeWarriorEntity) {
		return Identifier.of(BookOfE.MOD_ID, "textures/entity/slime_warrior/slime_warrior.png");
	}

	@Override
	public Identifier getAnimationResource(SlimeWarriorEntity slimeWarriorEntity) {
		return Identifier.of(BookOfE.MOD_ID, "animations/slime_common/slime_common.animation.json");
	}
}

