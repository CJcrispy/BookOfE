package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class BeachBladeItem extends SwordItem {
	// Movement tracking for Tidal Flow passive
	private static class MovementRecord {
		final Vec3d position;
		final int tick;

		MovementRecord(Vec3d position, int tick) {
			this.position = position;
			this.tick = tick;
		}
	}

	private static final Map<UUID, List<MovementRecord>> playerMovementHistory = new HashMap<>();
	private static final Map<UUID, Boolean> playerHasMomentum = new HashMap<>();
	private static final Map<UUID, SurfWave> activeSurfWaves = new HashMap<>();
	private static final Map<UUID, SurfMomentum> activeSurfMomentum = new HashMap<>();

	// Surf wave data
	private static class SurfWave {
		Vec3d position;
		Vec3d direction;
		double distanceTraveled;
		int ticksRemaining;
		final Set<Integer> damagedEntities;

		SurfWave(Vec3d startPos, Vec3d direction) {
			this.position = startPos;
			this.direction = direction.normalize();
			this.distanceTraveled = 0.0;
			this.ticksRemaining = WeaponConfig.BeachBlade.SURF_DURATION_TICKS;
			this.damagedEntities = new HashSet<>();
		}
	}

	// Surf momentum data (continues velocity after wave ends)
	private static class SurfMomentum {
		Vec3d direction;
		int ticksRemaining;

		SurfMomentum(Vec3d direction) {
			this.direction = direction.normalize();
			this.ticksRemaining = WeaponConfig.BeachBlade.SURF_MOMENTUM_PERSISTENCE_TICKS;
		}
	}

	public BeachBladeItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.getWorld().isClient() && attacker instanceof PlayerEntity player) {
			// Passive: Tidal Flow - +20% damage if player has momentum
			boolean hasMomentum = playerHasMomentum.getOrDefault(player.getUuid(), false);
			
			if (hasMomentum) {
				// Calculate base damage and apply multiplier
				float baseDamage = (float) attacker.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
				float bonusDamage = baseDamage * (WeaponConfig.BeachBlade.TIDAL_FLOW_DAMAGE_MULTIPLIER - 1.0f);
				
				// Apply bonus damage
				if (bonusDamage > 0) {
					target.damage(attacker.getDamageSources().playerAttack(player), bonusDamage);
				}

			// Create splash particles on final hit
			if (attacker.getWorld() instanceof ServerWorld serverWorld) {
				Vec3d pos = target.getPos();
				serverWorld.spawnParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.2, 0.3, 0.1);
				serverWorld.spawnParticles(ParticleTypes.BUBBLE, pos.x, pos.y + 0.3, pos.z, 5, 0.25, 0.1, 0.25, 0.05);
			}
			}
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		// Respect cooldown
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}

		// Apply cooldown immediately
		user.getItemCooldownManager().set(this, WeaponConfig.BeachBlade.SURF_COOLDOWN_TICKS);

		// Extinguish fire on the player
		user.extinguish();

		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		// Create Surf wave
		ServerWorld serverWorld = (ServerWorld) world;
		Vec3d look = user.getRotationVector().normalize();
		if (look.lengthSquared() < 1.0e-6) {
			look = new Vec3d(0, 0, 1);
		}

		// Start position slightly in front of player
		Vec3d startPos = user.getPos().add(look.multiply(1.0));
		SurfWave wave = new SurfWave(startPos, look);
		activeSurfWaves.put(user.getUuid(), wave);

		// Sound cue
		world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 0.8f, 1.0f);

		// Start wave movement tick
		tickSurfWave(serverWorld, wave, user);

		return TypedActionResult.success(stack, false);
	}

	// Called every tick to update surf waves
	public static void tickSurfWaves(ServerWorld world) {
		Iterator<Map.Entry<UUID, SurfWave>> it = activeSurfWaves.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, SurfWave> entry = it.next();
			UUID playerId = entry.getKey();
			SurfWave wave = entry.getValue();

			PlayerEntity player = world.getPlayerByUuid(playerId);
			if (player == null || !player.isAlive()) {
				it.remove();
				activeSurfMomentum.remove(playerId);
				continue;
			}

			tickSurfWave(world, wave, player);

			wave.ticksRemaining--;
			if (wave.ticksRemaining <= 0 || wave.distanceTraveled >= WeaponConfig.BeachBlade.SURF_DISTANCE_BLOCKS) {
				// Wave ended, start momentum persistence
				if (!activeSurfMomentum.containsKey(playerId)) {
					activeSurfMomentum.put(playerId, new SurfMomentum(wave.direction));
				}
				it.remove();
			}
		}

		// Tick momentum persistence (continues velocity after wave)
		Iterator<Map.Entry<UUID, SurfMomentum>> momentumIt = activeSurfMomentum.entrySet().iterator();
		while (momentumIt.hasNext()) {
			Map.Entry<UUID, SurfMomentum> entry = momentumIt.next();
			UUID playerId = entry.getKey();
			SurfMomentum momentum = entry.getValue();

			PlayerEntity player = world.getPlayerByUuid(playerId);
			if (player == null || !player.isAlive()) {
				momentumIt.remove();
				continue;
			}

			// Continue applying forward velocity
			Vec3d push = momentum.direction.multiply(WeaponConfig.BeachBlade.SURF_VELOCITY_STRENGTH * 0.6); // Slightly reduced after wave
			player.addVelocity(push.x, 0.05, push.z);
			player.velocityModified = true;

			// Particles under player during momentum
			Vec3d playerPos = player.getPos();
			world.spawnParticles(ParticleTypes.SPLASH, playerPos.x, playerPos.y - 0.1, playerPos.z, 4, 0.3, 0.05, 0.3, 0.05);
			world.spawnParticles(ParticleTypes.BUBBLE, playerPos.x, playerPos.y - 0.2, playerPos.z, 3, 0.25, 0.02, 0.25, 0.02);

			momentum.ticksRemaining--;
			if (momentum.ticksRemaining <= 0) {
				momentumIt.remove();
			}
		}
	}

	private static void tickSurfWave(ServerWorld world, SurfWave wave, PlayerEntity player) {
		// Move wave forward
		Vec3d move = wave.direction.multiply(WeaponConfig.BeachBlade.SURF_SPEED);
		wave.position = wave.position.add(move);
		wave.distanceTraveled += WeaponConfig.BeachBlade.SURF_SPEED;

		// Check for collisions with entities
		Box hitBox = new Box(
			wave.position.x - WeaponConfig.BeachBlade.SURF_WIDTH / 2,
			wave.position.y - 0.5,
			wave.position.z - WeaponConfig.BeachBlade.SURF_WIDTH / 2,
			wave.position.x + WeaponConfig.BeachBlade.SURF_WIDTH / 2,
			wave.position.y + 1.5,
			wave.position.z + WeaponConfig.BeachBlade.SURF_WIDTH / 2
		);

		List<Entity> entities = world.getOtherEntities(player, hitBox, e -> e instanceof LivingEntity && e.isAttackable());
		for (Entity e : entities) {
			if (e.getId() == player.getId()) continue;
			if (wave.damagedEntities.add(e.getId()) && e instanceof LivingEntity living) {
				living.damage(player.getDamageSources().playerAttack(player), WeaponConfig.BeachBlade.SURF_DAMAGE);
			}
		}

		// Visual effects: water wave particles at wave position
		world.spawnParticles(ParticleTypes.SPLASH, wave.position.x, wave.position.y + 0.3, wave.position.z, 12, 
			WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.3, WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.1);
		world.spawnParticles(ParticleTypes.BUBBLE, wave.position.x, wave.position.y + 0.2, wave.position.z, 8,
			WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.2, WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.05);
		world.spawnParticles(ParticleTypes.CLOUD, wave.position.x, wave.position.y + 0.4, wave.position.z, 3,
			WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.1, WeaponConfig.BeachBlade.SURF_WIDTH / 2, 0.02);

		// Carry player forward if they're near the wave
		double distanceToWave = player.squaredDistanceTo(wave.position);
		Vec3d playerPos = player.getPos();
		
		// Always apply strong forward velocity when surfing (stronger push)
		Vec3d push = wave.direction.multiply(WeaponConfig.BeachBlade.SURF_VELOCITY_STRENGTH);
		player.addVelocity(push.x, 0.1, push.z);
		player.velocityModified = true;

		// Blue/water particles under the player's feet to show surf path
		world.spawnParticles(ParticleTypes.SPLASH, playerPos.x, playerPos.y - 0.1, playerPos.z, 6, 0.4, 0.05, 0.4, 0.08);
		world.spawnParticles(ParticleTypes.BUBBLE, playerPos.x, playerPos.y - 0.2, playerPos.z, 5, 0.35, 0.03, 0.35, 0.05);
		world.spawnParticles(ParticleTypes.DRIPPING_WATER, playerPos.x, playerPos.y - 0.15, playerPos.z, 3, 0.3, 0.02, 0.3, 0.03);
		
		// Trail particles behind player showing the path
		Vec3d trailPos = playerPos.subtract(wave.direction.multiply(0.5));
		world.spawnParticles(ParticleTypes.SPLASH, trailPos.x, trailPos.y - 0.1, trailPos.z, 4, 0.25, 0.05, 0.25, 0.05);

		// Keep player slightly above the wave if needed
		if (distanceToWave < 9.0 && playerPos.y < wave.position.y + 0.5) {
			player.addVelocity(0, 0.15, 0);
		}
	}

	// Track player movement for Tidal Flow passive
	public static void trackPlayerMovement(PlayerEntity player) {
		UUID playerId = player.getUuid();
		Vec3d currentPos = player.getPos();
		int currentTick = (int) player.getWorld().getTime();

		// Get or create movement history
		List<MovementRecord> history = playerMovementHistory.computeIfAbsent(playerId, k -> new ArrayList<>());

		// Add current position
		history.add(new MovementRecord(currentPos, currentTick));

		// Remove old records (older than tracking window)
		history.removeIf(record -> currentTick - record.tick > WeaponConfig.BeachBlade.TIDAL_FLOW_TRACKING_WINDOW_TICKS);

		// Calculate total distance moved in the window
		double totalDistance = 0.0;
		if (history.size() >= 2) {
			for (int i = 1; i < history.size(); i++) {
				MovementRecord prev = history.get(i - 1);
				MovementRecord curr = history.get(i);
				totalDistance += prev.position.distanceTo(curr.position);
			}
		}

		// Update momentum status
		boolean hasMomentum = totalDistance >= WeaponConfig.BeachBlade.TIDAL_FLOW_MIN_DISTANCE;
		playerHasMomentum.put(playerId, hasMomentum);
	}

	public static boolean hasMomentum(UUID playerId) {
		return playerHasMomentum.getOrDefault(playerId, false);
	}

	// Clean up when player disconnects
	public static void cleanupPlayer(UUID playerId) {
		playerMovementHistory.remove(playerId);
		playerHasMomentum.remove(playerId);
		activeSurfWaves.remove(playerId);
		activeSurfMomentum.remove(playerId);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.beach_blade.passive").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.translatable("item.bookofe.beach_blade.passive.desc").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.bookofe.beach_blade.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.beach_blade.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}
