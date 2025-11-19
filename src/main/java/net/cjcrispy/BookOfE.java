package net.cjcrispy;

import net.cjcrispy.block.ModBlocks;
import net.cjcrispy.config.SlimeNameConfig;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.*;
import net.cjcrispy.entity.summoning.AltarRitual;
import net.cjcrispy.entity.summoning.BossSummoningInitializer;
import net.cjcrispy.enchant.ModEnchantments;
import net.cjcrispy.effect.ModEffects;
import net.cjcrispy.item.ModItemGroups;
import net.cjcrispy.item.ModItems;
import net.cjcrispy.item.custom.BeachBladeItem;
import net.cjcrispy.item.custom.MoonveilSwordItem;
import net.cjcrispy.item.custom.SlimeHammerItem;
import net.cjcrispy.item.custom.WormHammerItem;
import net.cjcrispy.procedure.wormhammer.TremorHit;
import net.cjcrispy.rift.SeveringRiftManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BookOfE implements ModInitializer {
	public static final String MOD_ID = "bookofe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static final Set<UUID> playersToGrantRoot = new HashSet<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing {}", MOD_ID);
		
		registerModContent();
		registerEntityAttributes();
		registerBiomeSpawns();
		registerBossSummonings();
		registerEventHandlers();
		registerAdvancementHandlers();
	}
	
	private void registerModContent() {
		// Force entity class loading for spawn eggs
		@SuppressWarnings("unused")
		var dummyEntity = ModEntities.MILLY_KNIGHT;
		
		ModEntities.registerModEntities();
		ModEffects.registerModEffects();
		ModEnchantments.registerModEnchantments();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();
		SlimeNameConfig.load();
	}
	
	private void registerEntityAttributes() {
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
	}
	
	private void registerBiomeSpawns() {
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(
				RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "swamp")),
				RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "dark_forest"))
			),
			SpawnGroup.MONSTER,
			ModEntities.DARK_WIZARD,
			80, 1, 2
		);
	}
	
	private void registerBossSummonings() {
		BossSummoningInitializer.register();
	}
	
	private void registerEventHandlers() {
		// Worm Hammer attack tracking
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof PlayerEntity && entity instanceof LivingEntity livingTarget) {
				ItemStack stack = player.getStackInHand(hand);
				
				if (stack.getItem() instanceof WormHammerItem) {
					WormHammerItem.incrementHitCount(player.getUuid());
					int hitCount = WormHammerItem.getHitCount(player.getUuid());
					
					if (hitCount >= 3) {
						WormHammerItem.setHitCount(player.getUuid(), 0);
						TremorHit.execute((PlayerEntity) player, livingTarget);
					}
				}
				
			}
			return ActionResult.PASS;
		});
		
		// World tick handler
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.isClient()) return;
			
			// Handle weapon cooldowns and tracking
			world.getPlayers().forEach(player -> {
				for (Hand hand : Hand.values()) {
					ItemStack stack = player.getStackInHand(hand);
					if (stack.getItem() instanceof WormHammerItem) {
						WormHammerItem.tickCooldown(player.getUuid());
					}
				}
			});
			
			if (world instanceof ServerWorld serverWorld) {
				SeveringRiftManager.tickWorld(serverWorld);
				BeachBladeItem.tickRiptideDashes(serverWorld);
				SlimeHammerItem.tickSlimeMinions(serverWorld);
				MoonveilSwordItem.tickMoonfallGlows(serverWorld);
				AltarRitual.tickRituals(serverWorld);
			}
		});
		
		// Player disconnect cleanup
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.player.getUuid();
			BeachBladeItem.cleanupPlayer(playerId);
			SlimeHammerItem.cleanupPlayer(playerId);
			MoonveilSwordItem.cleanupPlayer(playerId);
		});
	}
	
	private void registerAdvancementHandlers() {
		// Mark players for root advancement on join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			playersToGrantRoot.add(handler.player.getUuid());
		});
		
		// Grant root advancement
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.isClient() || playersToGrantRoot.isEmpty()) return;
			
			Identifier rootId = Identifier.of("bookofe", "root");
			AdvancementEntry rootAdvancement = world.getServer().getAdvancementLoader().get(rootId);
			
			if (rootAdvancement == null) {
				playersToGrantRoot.clear();
				return;
			}
			
			playersToGrantRoot.removeIf(playerUuid -> {
				PlayerEntity player = world.getPlayerByUuid(playerUuid);
				if (player instanceof ServerPlayerEntity serverPlayer) {
					PlayerAdvancementTracker tracker = serverPlayer.getAdvancementTracker();
					
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
		});
	}
}
