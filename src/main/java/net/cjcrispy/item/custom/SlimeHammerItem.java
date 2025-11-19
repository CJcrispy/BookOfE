package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.config.SlimeNameConfig;
import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.SlimeCommonEntity;
import net.cjcrispy.entity.custom.SlimeMageEntity;
import net.cjcrispy.entity.custom.SlimeWarriorEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.*;

public class SlimeHammerItem extends SwordItem {
	// Track active slime minions per player
	private static final Map<UUID, List<SlimeMinionData>> activeMinions = new HashMap<>();

	// Data for each slime minion
	private static class SlimeMinionData {
		final UUID slimeId;
		int ticksRemaining;
		final UUID ownerId;

		SlimeMinionData(UUID slimeId, int ticksRemaining, UUID ownerId) {
			this.slimeId = slimeId;
			this.ticksRemaining = ticksRemaining;
			this.ownerId = ownerId;
		}
	}

	// Helper method to create a random slime type
	private static LivingEntity createRandomSlime(ServerWorld world, Random random) {
		int type = random.nextInt(3);
		return switch (type) {
			case 0 -> new SlimeCommonEntity(ModEntities.SLIME_COMMON, world);
			case 1 -> new SlimeMageEntity(ModEntities.SLIME_MAGE, world);
			case 2 -> new SlimeWarriorEntity(ModEntities.SLIME_WARRIOR, world);
			default -> new SlimeCommonEntity(ModEntities.SLIME_COMMON, world);
		};
	}

	public SlimeHammerItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		// Respect cooldown
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}

		// Apply cooldown immediately
		user.getItemCooldownManager().set(this, WeaponConfig.SlimeHammer.SLIME_SPLIT_COOLDOWN_TICKS);

		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		// Spawn 2-3 tiny slime minions
		ServerWorld serverWorld = (ServerWorld) world;
		Random random = world.getRandom();
		int minionCount = 2 + random.nextInt(2); // 2 or 3

		List<SlimeMinionData> playerMinions = activeMinions.computeIfAbsent(user.getUuid(), k -> new ArrayList<>());

		for (int i = 0; i < minionCount; i++) {
			LivingEntity slime = createRandomSlime(serverWorld, random);
			slime.setPosition(user.getX() + (random.nextDouble() - 0.5) * 2.0, 
				user.getY() + 0.5, 
				user.getZ() + (random.nextDouble() - 0.5) * 2.0);
			
			// Set owner and make it friendly to player
			// Set custom name and owner UUID based on slime type
			if (slime instanceof SlimeCommonEntity commonSlime) {
				commonSlime.setCustomName(Text.literal(SlimeNameConfig.getRandomCommonerName(random)));
				commonSlime.setOwnerUuid(user.getUuid());
			} else if (slime instanceof SlimeMageEntity mageSlime) {
				mageSlime.setCustomName(Text.literal(SlimeNameConfig.getRandomMageName(random)));
				mageSlime.setOwnerUuid(user.getUuid());
			} else if (slime instanceof SlimeWarriorEntity warriorSlime) {
				warriorSlime.setCustomName(Text.literal(SlimeNameConfig.getRandomWarriorName(random)));
				warriorSlime.setOwnerUuid(user.getUuid());
			}

			// Spawn the slime
			serverWorld.spawnEntity(slime);
			
			// Track the minion
			playerMinions.add(new SlimeMinionData(slime.getUuid(), WeaponConfig.SlimeHammer.SLIME_SPLIT_DURATION_TICKS, user.getUuid()));
		}

		// Sound cue
		world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.8f, 1.5f);

		// Particle effect on spawn
		Vec3d pos = user.getPos();
		serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y + 1.0, pos.z, 15, 0.5, 0.5, 0.5, 0.1);

		return TypedActionResult.success(stack, false);
	}

	// Called every tick to update slime minions
	public static void tickSlimeMinions(ServerWorld world) {
		Iterator<Map.Entry<UUID, List<SlimeMinionData>>> it = activeMinions.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, List<SlimeMinionData>> entry = it.next();
			List<SlimeMinionData> minions = entry.getValue();
			
			Iterator<SlimeMinionData> minionIt = minions.iterator();
			while (minionIt.hasNext()) {
				SlimeMinionData data = minionIt.next();
				data.ticksRemaining--;

				Entity slimeEntity = world.getEntity(data.slimeId);
				if (slimeEntity == null || !slimeEntity.isAlive() || data.ticksRemaining <= 0) {
					// Minion expired or died
					if (slimeEntity != null && slimeEntity.isAlive()) {
						// Spawn particles on expiration
						Vec3d pos = slimeEntity.getPos();
						world.spawnParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
						world.playSound(null, slimeEntity.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.NEUTRAL, 0.5f, 1.2f);
						slimeEntity.remove(Entity.RemovalReason.DISCARDED);
					}
					minionIt.remove();
				}
			}

			// Remove empty lists
			if (minions.isEmpty()) {
				it.remove();
			}
		}
	}

	// Check if a slime is a minion owned by a specific player
	public static boolean isSlimeMinion(UUID slimeId, UUID ownerId) {
		for (List<SlimeMinionData> minions : activeMinions.values()) {
			for (SlimeMinionData data : minions) {
				if (data.slimeId.equals(slimeId) && data.ownerId.equals(ownerId)) {
					return true;
				}
			}
		}
		return false;
	}

	// Clean up when player disconnects
	public static void cleanupPlayer(UUID playerId) {
		activeMinions.remove(playerId);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.slime_hammer.passive").formatted(Formatting.DARK_GREEN));
		tooltip.add(Text.translatable("item.bookofe.slime_hammer.passive.desc").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.bookofe.slime_hammer.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.slime_hammer.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}

