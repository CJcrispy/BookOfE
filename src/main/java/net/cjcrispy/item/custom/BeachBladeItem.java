package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class BeachBladeItem extends SwordItem {
	private static final Map<UUID, Integer> cooldowns = new HashMap<>();
	private static final Map<UUID, RiptideDash> activeDashes = new HashMap<>();
	private static final Map<Integer, Integer> soakedEntities = new HashMap<>(); // entity ID -> remaining ticks

	// Track an active dash
	private static class RiptideDash {
		final Vec3d startPos;
		double distanceTraveled;
		boolean hasHitTarget;
		int ticks;

		RiptideDash(Vec3d startPos) {
			this.startPos = startPos;
			this.distanceTraveled = 0.0;
			this.hasHitTarget = false;
			this.ticks = 0;
		}
	}

	public BeachBladeItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		if (!(world instanceof ServerWorld serverWorld)) {
			return TypedActionResult.pass(stack);
		}

		UUID playerId = user.getUuid();

		// Check cooldown
		int cooldown = cooldowns.getOrDefault(playerId, 0);
		if (cooldown > 0) {
			return TypedActionResult.fail(stack);
		}

		// Check if already dashing
		if (activeDashes.containsKey(playerId)) {
			return TypedActionResult.fail(stack);
		}

		// Get player's look direction (horizontal)
		Vec3d lookVec = user.getRotationVector();
		Vec3d direction = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
		if (direction.lengthSquared() < 0.01) {
			direction = new Vec3d(0, 0, 1); // Default forward
		}

		// Start dash
		activeDashes.put(playerId, new RiptideDash(user.getPos()));

		// Apply initial velocity
		Vec3d velocity = direction.multiply(WeaponConfig.BeachBlade.RIPTIDE_CRASH_SPEED);
		velocity = new Vec3d(velocity.x, velocity.y + 0.3, velocity.z); // Slight upward component
		user.setVelocity(velocity);
		user.velocityModified = true;

		// Sound and particles
		serverWorld.playSound(null, user.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1.value(), SoundCategory.PLAYERS, 1.0f, 1.2f);
		serverWorld.spawnParticles(ParticleTypes.SPLASH, user.getX(), user.getY(), user.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

		return TypedActionResult.success(stack, false);
	}

	// Called every tick to update active dashes
	public static void tickRiptideDashes(ServerWorld world) {
		// Tick cooldowns
		cooldowns.replaceAll((uuid, cooldown) -> Math.max(0, cooldown - 1));
		cooldowns.entrySet().removeIf(e -> e.getValue() <= 0);

		// Tick soaked effect durations
		soakedEntities.replaceAll((entityId, ticks) -> Math.max(0, ticks - 1));
		soakedEntities.entrySet().removeIf(e -> e.getValue() <= 0);

		Iterator<Map.Entry<UUID, RiptideDash>> it = activeDashes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, RiptideDash> entry = it.next();
			UUID playerId = entry.getKey();
			RiptideDash dash = entry.getValue();

			PlayerEntity player = world.getPlayerByUuid(playerId);
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}

			dash.ticks++;
			Vec3d currentPos = player.getPos();
			dash.distanceTraveled = dash.startPos.distanceTo(new Vec3d(currentPos.x, dash.startPos.y, currentPos.z));

			// Check for entity collisions (only if hasn't hit yet)
			if (!dash.hasHitTarget) {
				Box hitBox = player.getBoundingBox().expand(WeaponConfig.BeachBlade.RIPTIDE_CRASH_HIT_RANGE);
				List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, hitBox,
					e -> e.isAlive() && e.isAttackable() && e != player);

				if (!nearbyEntities.isEmpty()) {
					LivingEntity hitTarget = nearbyEntities.get(0); // First entity hit
					dash.hasHitTarget = true;

					// Deal bonus damage
					hitTarget.damage(player.getDamageSources().playerAttack(player), WeaponConfig.BeachBlade.RIPTIDE_CRASH_DAMAGE);

					// Slight knockup
					Vec3d knockup = new Vec3d(0, 0.4, 0);
					hitTarget.addVelocity(knockup.x, knockup.y, knockup.z);
					hitTarget.velocityModified = true;

					// Medium knockback
					Vec3d knockbackDir = hitTarget.getPos().subtract(currentPos).normalize();
					Vec3d knockback = knockbackDir.multiply(0.6);
					hitTarget.addVelocity(knockback.x, 0, knockback.z);
					hitTarget.velocityModified = true;

					// Apply Soaked effect (Slowness I + fire resistance tracking)
					hitTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 
						WeaponConfig.BeachBlade.RIPTIDE_CRASH_SOAKED_DURATION_TICKS, 0, false, false));
					soakedEntities.put(hitTarget.getId(), WeaponConfig.BeachBlade.RIPTIDE_CRASH_SOAKED_DURATION_TICKS);

					// Particles and sound
					world.spawnParticles(ParticleTypes.SPLASH, hitTarget.getX(), hitTarget.getY() + 0.5, hitTarget.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
					world.playSound(null, hitTarget.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.NEUTRAL, 0.8f, 1.0f);
				}
			}

			// Extinguish fire and lava along path
			extinguishFireAndLava(world, player, currentPos);

			// Splash particles behind player
			if (dash.ticks % 2 == 0) {
				world.spawnParticles(ParticleTypes.SPLASH, currentPos.x, currentPos.y, currentPos.z, 3, 0.2, 0.1, 0.2, 0.05);
			}

			// Check if dash should end
			if (dash.distanceTraveled >= WeaponConfig.BeachBlade.RIPTIDE_CRASH_DISTANCE || 
				player.isOnGround() && dash.ticks > 5) {
				// Dash complete - apply Dolphin's Grace
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 
					WeaponConfig.BeachBlade.RIPTIDE_CRASH_DOLPHIN_GRACE_TICKS, 0, false, false));

				// Set cooldown
				cooldowns.put(playerId, WeaponConfig.BeachBlade.RIPTIDE_CRASH_COOLDOWN_TICKS);

				// Final splash particles
				world.spawnParticles(ParticleTypes.SPLASH, currentPos.x, currentPos.y, currentPos.z, 30, 0.5, 0.3, 0.5, 0.2);
				world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 0.8f, 1.0f);

				it.remove();
			}
		}
	}

	private static void extinguishFireAndLava(ServerWorld world, PlayerEntity player, Vec3d pos) {
		BlockPos blockPos = BlockPos.ofFloored(pos);

		// Check blocks in a small area around player
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					BlockPos checkPos = blockPos.add(x, y, z);
					Block block = world.getBlockState(checkPos).getBlock();

					if (block == Blocks.FIRE) {
						world.setBlockState(checkPos, Blocks.AIR.getDefaultState(), 3);
						world.spawnParticles(ParticleTypes.CLOUD, checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01);
					} else if (block == Blocks.LAVA) {
						world.setBlockState(checkPos, Blocks.WATER.getDefaultState(), 3);
						world.spawnParticles(ParticleTypes.SMOKE, checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
					}
				}
			}
		}

		// Extinguish player if on fire
		if (player.isOnFire()) {
			player.extinguish();
		}
	}

	// Check if entity is soaked (for fire damage reduction in mixin)
	public static boolean isSoaked(int entityId) {
		return soakedEntities.containsKey(entityId) && soakedEntities.get(entityId) > 0;
	}

	// Clean up when player disconnects
	public static void cleanupPlayer(UUID playerId) {
		cooldowns.remove(playerId);
		activeDashes.remove(playerId);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.beach_blade.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.beach_blade.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}
