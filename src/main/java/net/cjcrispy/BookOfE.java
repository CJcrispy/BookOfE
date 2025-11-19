package net.cjcrispy;

import net.cjcrispy.block.ModBlocks;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.*;
import net.cjcrispy.entity.summoning.AltarRitual;
import net.cjcrispy.entity.summoning.BossSummoningInitializer;
import net.cjcrispy.item.ModItemGroups;
import net.cjcrispy.item.ModItems;
import net.cjcrispy.item.custom.WormHammerItem;
import net.cjcrispy.item.custom.BeachBladeItem;
import net.cjcrispy.item.custom.SlimeHammerItem;
import net.cjcrispy.item.custom.MoonveilSwordItem;
import net.cjcrispy.config.SlimeNameConfig;
import net.cjcrispy.effect.ModEffects;
import net.cjcrispy.enchant.ModEnchantments;
import net.cjcrispy.rift.SeveringRiftManager;
import net.cjcrispy.procedure.wormhammer.TremorHit;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookOfE implements ModInitializer {
	public static final String MOD_ID = "bookofe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register in proper order: entities first (needed for spawn eggs), then items/blocks, then groups
		// Force class loading by accessing static fields to ensure proper initialization order
		
		// Load entities first (needed for spawn eggs)
		// Force entity class to load by accessing a static field
		@SuppressWarnings("unused")
		net.minecraft.entity.EntityType<?> dummyEntity = ModEntities.MILLY_KNIGHT;
		ModEntities.registerModEntities();
		
		// Load effects
		ModEffects.registerModEffects();
		
		// Load enchantments
		ModEnchantments.registerModEnchantments();
		
		// Now register items and blocks (spawn eggs are registered in registerModItems after entities are loaded)
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		
		// Now safe to register item groups that reference items/blocks
		ModItemGroups.registerItemGroups();
		
		// Load slime name configurations
		SlimeNameConfig.load();

		FabricDefaultAttributeRegistry.register(ModEntities.MILLY_KNIGHT, MillyKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.NICKY_SUMMONER, NickySummonerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JOE_REBEL, JoeRebelEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JOE_REBEL_CLONE, JoeRebelCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.CHRIS_SLIME, SlimeChrisEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.KING_HAJILE, KingHajileEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.DARK_WIZARD, DarkWizardEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.BLACKBIRD_WARRIOR, BlackBirdEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SHADOW_QUINN, ShadowQuinnEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SLIME_COMMON, SlimeCommonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SLIME_MAGE, SlimeMageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SLIME_WARRIOR, SlimeWarriorEntity.createAttributes());

		// Register DarkWizard spawns in Swamps and Dark Forests
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(
				net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, net.minecraft.util.Identifier.of("minecraft", "swamp")),
				net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, net.minecraft.util.Identifier.of("minecraft", "dark_forest"))
			),
			SpawnGroup.MONSTER,
			ModEntities.DARK_WIZARD,
			80, // weight (higher = more common)
			1,  // minCount
			2   // maxCount
		);

		// Register boss summonings
		BossSummoningInitializer.register();
		
		// Register Worm Hammer attack tracking for Tremor Hit
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof PlayerEntity && entity instanceof LivingEntity livingTarget) {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.getItem() instanceof WormHammerItem) {
					WormHammerItem.incrementHitCount(player.getUuid());
					int hitCount = WormHammerItem.getHitCount(player.getUuid());
					
					// Every third hit triggers Tremor Hit
					if (hitCount >= 3) {
						WormHammerItem.setHitCount(player.getUuid(), 0); // Reset counter
						TremorHit.execute((PlayerEntity) player, livingTarget);
					}
				}
			}
			return ActionResult.PASS;
		});
		
		// Register combined tick handler for weapon abilities and rituals
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!world.isClient()) {
				world.getPlayers().forEach(player -> {
					for (Hand hand : Hand.values()) {
						ItemStack stack = player.getStackInHand(hand);
						if (stack.getItem() instanceof WormHammerItem) {
							WormHammerItem.tickCooldown(player.getUuid());
						}
						// Track movement for Beach Blade Tidal Flow passive
						if (stack.getItem() instanceof BeachBladeItem) {
							BeachBladeItem.trackPlayerMovement(player);
						}
					}
				});
				if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
					SeveringRiftManager.tickWorld(serverWorld);
					// Tick Surf waves
					BeachBladeItem.tickSurfWaves(serverWorld);
					// Tick Slime minions
					SlimeHammerItem.tickSlimeMinions(serverWorld);
					// Tick Moonfall glows
					MoonveilSwordItem.tickMoonfallGlows(serverWorld);
					// Tick rituals
					AltarRitual.tickRituals(serverWorld);
				}
			}
		});
		
		// Track players who need the root advancement
		java.util.Set<java.util.UUID> playersToGrant = new java.util.HashSet<>();
		
		// Mark players for advancement grant on join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			playersToGrant.add(handler.player.getUuid());
		});

		// Clean up Beach Blade data on disconnect
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BeachBladeItem.cleanupPlayer(handler.player.getUuid());
			SlimeHammerItem.cleanupPlayer(handler.player.getUuid());
			MoonveilSwordItem.cleanupPlayer(handler.player.getUuid());
		});
		
		// Grant root advancement on world tick (ensures advancement loader is ready)
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!world.isClient() && !playersToGrant.isEmpty()) {
				net.minecraft.util.Identifier rootId = net.minecraft.util.Identifier.of("bookofe", "root");
				net.minecraft.advancement.AdvancementEntry rootAdvancement = world.getServer().getAdvancementLoader().get(rootId);
				
				if (rootAdvancement == null) {
					playersToGrant.clear();
					return;
				}
				
				playersToGrant.removeIf(playerUuid -> {
					net.minecraft.entity.player.PlayerEntity player = world.getPlayerByUuid(playerUuid);
					if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
						net.minecraft.advancement.PlayerAdvancementTracker tracker = serverPlayer.getAdvancementTracker();
						
						if (!tracker.getProgress(rootAdvancement).isDone()) {
							tracker.grantCriterion(rootAdvancement, "requirement");
							serverPlayer.sendMessage(
								net.minecraft.text.Text.translatable("advancement.bookofe.root.welcome")
									.formatted(net.minecraft.util.Formatting.DARK_PURPLE),
								false
							);
						}
						return true;
					}
					return true;
				});
			}
		});
	}
}