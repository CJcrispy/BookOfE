package net.cjcrispy.entity.client.quinn;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.ShadowQuinnEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ShadowQuinnModel extends GeoModel<ShadowQuinnEntity> {

    @Override
    public Identifier getModelResource(ShadowQuinnEntity shadowQuinnEntity) {
        return Identifier.of(BookOfE.MOD_ID, "geo/shadow_quinn/shadow_quinn.geo.json");
    }

    @Override
    public Identifier getTextureResource(ShadowQuinnEntity shadowQuinnEntity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/quinn/shadowquinn.png");
    }

    @Override
    public Identifier getAnimationResource(ShadowQuinnEntity shadowQuinnEntity) {
        return Identifier.of(BookOfE.MOD_ID, "animations/shadow_quinn/shadow_quinn.animation.json");
    }
}