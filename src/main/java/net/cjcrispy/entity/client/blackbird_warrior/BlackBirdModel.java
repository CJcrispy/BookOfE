package net.cjcrispy.entity.client.blackbird_warrior;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.BlackBirdEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BlackBirdModel extends GeoModel<BlackBirdEntity> {

    @Override
    public Identifier getModelResource(BlackBirdEntity blackBirdEntity) {
        return Identifier.of(BookOfE.MOD_ID, "geo/blackbird/blackbird_warrior.geo.json");
    }

    @Override
    public Identifier getTextureResource(BlackBirdEntity blackBirdEntity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/blackbird_warrior/blackbird_warrior.png");
    }

    @Override
    public Identifier getAnimationResource(BlackBirdEntity blackBirdEntity) {
        return Identifier.of(BookOfE.MOD_ID, "animations/blackbird/blackbird_warrior.animation.json");
    }
}
