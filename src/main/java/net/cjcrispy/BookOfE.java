package net.cjcrispy;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.DarkWizardEntity;
import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.item.ModItemGroups;
import net.cjcrispy.item.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookOfE implements ModInitializer {
	public static final String MOD_ID = "bookofe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();

		ModItems.registerModItems();
		ModEntities.registerModEntities();

		FabricDefaultAttributeRegistry.register(ModEntities.MILLY_KNIGHT, MillyKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.DARK_WIZARD, DarkWizardEntity.createAttributes());
	}
}