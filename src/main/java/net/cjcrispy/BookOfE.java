package net.cjcrispy;

import net.cjcrispy.block.ModBlocks;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.*;
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
		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();

		FabricDefaultAttributeRegistry.register(ModEntities.MILLY_KNIGHT, MillyKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.QUINN_KNIGHT, QuinnKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.NICKY_SUMMONER, NickySummonerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JOE_REBEL, JoeRebelEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.CHRIS_SLIME, SlimeChrisEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.KING_HAJILE, KingHajileEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.DARK_WIZARD, DarkWizardEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.BLACKBIRD_WARRIOR, BlackBirdEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SHADOW_QUINN, ShadowQuinnEntity.createAttributes());
	}
}