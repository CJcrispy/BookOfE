package net.cjcrispy.entity.client.dark_wizard;

import net.cjcrispy.BookOfE;
import net.cjcrispy.entity.custom.DarkWizardEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DarkWizardModel extends GeoModel<DarkWizardEntity> {
    @Override
    public Identifier getModelResource(DarkWizardEntity darkWizardEntity) {
        return Identifier.of(BookOfE.MOD_ID, "geo/dark_wizard/dark_wizard.geo.json");
    }

    @Override
    public Identifier getTextureResource(DarkWizardEntity darkWizardEntity) {
        return Identifier.of(BookOfE.MOD_ID, "textures/entity/dark_wizard/l_darkwizard.png");
    }

    @Override
    public Identifier getAnimationResource(DarkWizardEntity darkWizardEntity) {
        return Identifier.of(BookOfE.MOD_ID, "animations/dark_wizard/dark_wizard.animation.json");
    }


}
