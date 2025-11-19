package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

public class MoonveilSwordItem extends SwordItem {
	// Track active moonfall glow effects
	private static final Map<UUID, MoonfallGlow> activeGlows = new HashMap<>();

	// Data for lingering glow effect
	private static class MoonfallGlow {
		final BlockPos centerPos;
		int ticksRemaining;
		final UUID ownerId;

		MoonfallGlow(BlockPos centerPos, int ticksRemaining, UUID ownerId) {
			this.centerPos = centerPos;
			this.ticksRemaining = ticksRemaining;
			this.ownerId = ownerId;
		}
	}

	public MoonveilSwordItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	/**
	 * Passive: Lunar Edge - +20% damage at night or in open sky with moon visible
	 */
	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!target.getWorld().isClient() && attacker instanceof PlayerEntity player) {
			// Check if Lunar Edge conditions are met
			if (isLunarEdgeActive(player.getWorld(), player.getBlockPos())) {
				// Calculate base damage and apply multiplier
				float baseDamage = (float) attacker.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
				float bonusDamage = baseDamage * (WeaponConfig.MoonSword.LUNAR_EDGE_DAMAGE_MULTIPLIER - 1.0f);
				
				// Apply bonus damage
				if (bonusDamage > 0) {
					target.damage(attacker.getDamageSources().playerAttack(player), bonusDamage);
				}

				// Spawn white-blue particles on hit
				if (attacker.getWorld() instanceof ServerWorld serverWorld) {
					Vec3d pos = target.getPos();
					Vector3f moonColor = new Vector3f(0.7f, 0.8f, 1.0f); // White-blue color
					DustParticleEffect moonDust = new DustParticleEffect(moonColor, 1.0f);
					serverWorld.spawnParticles(moonDust, pos.x, pos.y + target.getHeight() / 2.0, pos.z, 8, 0.3, 0.2, 0.3, 0.05);
					serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + target.getHeight() / 2.0, pos.z, 3, 0.2, 0.2, 0.2, 0.02);
				}
			}
		}
		return super.postHit(stack, target, attacker);
	}

	/**
	 * Active: Moonfall - Sneak + Right-click to call down a beam of moonlight
	 */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		// Check if player is sneaking
		if (!user.isSneaking()) {
			return TypedActionResult.pass(stack);
		}

		// Respect cooldown
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}

		// Apply cooldown immediately
		user.getItemCooldownManager().set(this, WeaponConfig.MoonSword.MOONFALL_COOLDOWN_TICKS);

		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		// Find target position: first check for mob, then ground
		ServerWorld serverWorld = (ServerWorld) world;
		BlockPos targetPos;
		
		// First, try to find a mob the player is looking at
		LivingEntity targetMob = findTargetEntity(user, WeaponConfig.MoonSword.MOONFALL_RANGE);
		
		if (targetMob != null) {
			// Target the mob's position
			targetPos = BlockPos.ofFloored(targetMob.getPos());
		} else {
			// No mob found, raycast to find ground position
			Vec3d lookVec = user.getRotationVector();
			Vec3d startPos = user.getEyePos();
			Vec3d endPos = startPos.add(lookVec.multiply(WeaponConfig.MoonSword.MOONFALL_RANGE));
			targetPos = findGroundPosition(serverWorld, startPos, endPos);
		}
		
		// Execute Moonfall at target position
		executeMoonfall(serverWorld, user, targetPos);

		// Sound cue
		world.playSound(null, targetPos, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 1.2f);

		return TypedActionResult.success(stack, false);
	}

	/**
	 * Checks if Lunar Edge passive is active (night OR open sky with moon visible)
	 */
	private boolean isLunarEdgeActive(World world, BlockPos pos) {
		// Check if it's night (time between 13000 and 23000)
		long timeOfDay = world.getTimeOfDay() % 24000;
		boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;
		
		if (isNight) {
			return true;
		}

		// Check if moon is visible in open sky
		// Check if there's a clear path to the sky (no solid blocks above)
		for (int y = pos.getY() + 1; y < world.getTopY(); y++) {
			BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
			if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
				return false; // Blocked by solid block
			}
		}
		
		// If we reach here, sky is visible
		// Check if moon phase allows visibility (moon is always visible at night, but during day we check sky light)
		// For simplicity, if sky is visible and it's not full day, consider it active
		int skyLight = world.getLightLevel(LightType.SKY, pos);
		return skyLight < 15; // Moon visible when sky light is less than max
	}

	/**
	 * Finds the entity the player is looking at within the specified range
	 */
	private LivingEntity findTargetEntity(PlayerEntity player, double range) {
		Vec3d eyePos = player.getEyePos();
		Vec3d lookVec = player.getRotationVector();
		Vec3d endPos = eyePos.add(lookVec.multiply(range));
		
		// Get all entities in a box along the raycast path
		Box searchBox = new Box(eyePos, endPos).expand(1.0);
		java.util.function.Predicate<Entity> filter = entity -> 
			entity instanceof LivingEntity 
			&& entity.isAttackable() 
			&& entity != player
			&& !entity.isSpectator();
		
		List<Entity> entities = player.getWorld().getOtherEntities(player, searchBox, filter);
		
		LivingEntity closestTarget = null;
		double closestDistance = range * range;
		
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity living)) continue;
			
			// Check if entity is within the bounding box intersection
			Box entityBox = living.getBoundingBox();
			Vec3d hitPos = entityBox.raycast(eyePos, endPos).orElse(null);
			
			if (hitPos != null) {
				double distanceSq = eyePos.squaredDistanceTo(hitPos);
				if (distanceSq < closestDistance) {
					closestDistance = distanceSq;
					closestTarget = living;
				}
			}
		}
		
		return closestTarget;
	}

	/**
	 * Finds the ground position for Moonfall using raycast
	 */
	private BlockPos findGroundPosition(ServerWorld world, Vec3d startPos, Vec3d endPos) {
		// Manual raycast: step along the ray and check for solid blocks
		Vec3d direction = endPos.subtract(startPos);
		double distance = direction.length();
		if (distance < 0.1) {
			return BlockPos.ofFloored(startPos);
		}
		
		Vec3d normalized = direction.normalize();
		double stepSize = 0.5; // Check every 0.5 blocks
		int steps = (int) (distance / stepSize) + 1;
		
		// Check along the ray for the first solid block
		for (int i = 0; i < steps; i++) {
			double progress = (double) i / steps;
			Vec3d checkPos = startPos.add(normalized.multiply(distance * progress));
			BlockPos blockPos = BlockPos.ofFloored(checkPos);
			
			// Check if this block is solid
			if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
				// Return the position above the solid block
				return blockPos.up();
			}
		}
		
		// If no block found along the ray, find the first solid block below the end position
		BlockPos checkPos = BlockPos.ofFloored(endPos);
		for (int y = checkPos.getY(); y > world.getBottomY(); y--) {
			BlockPos pos = new BlockPos(checkPos.getX(), y, checkPos.getZ());
			if (world.getBlockState(pos).isSolidBlock(world, pos)) {
				return pos.up(); // Return position above the solid block
			}
		}
		
		return checkPos; // Fallback to original position
	}

	/**
	 * Executes the Moonfall ability at the target position
	 */
	private void executeMoonfall(ServerWorld world, PlayerEntity caster, BlockPos centerPos) {
		// Create 3×3 area
		int radius = 1;
		List<LivingEntity> affectedEntities = new ArrayList<>();
		
		// Find all entities in the 3×3 area
		Box areaBox = new Box(
			centerPos.getX() - radius, centerPos.getY() - 1, centerPos.getZ() - radius,
			centerPos.getX() + radius + 1, centerPos.getY() + 3, centerPos.getZ() + radius + 1
		);
		
		List<Entity> entities = world.getOtherEntities(caster, areaBox, 
			entity -> entity instanceof LivingEntity && entity.isAttackable());
		
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity living) {
				affectedEntities.add(living);
			}
		}

		// Apply effects to all entities in the area
		for (LivingEntity target : affectedEntities) {
			// Check if it's undead (zombie, skeleton, etc.)
			net.minecraft.entity.EntityType<?> entityType = target.getType();
			boolean isUndead = entityType == net.minecraft.entity.EntityType.ZOMBIE ||
				entityType == net.minecraft.entity.EntityType.SKELETON ||
				entityType == net.minecraft.entity.EntityType.WITHER_SKELETON ||
				entityType == net.minecraft.entity.EntityType.ZOMBIFIED_PIGLIN ||
				entityType == net.minecraft.entity.EntityType.DROWNED ||
				entityType == net.minecraft.entity.EntityType.PHANTOM ||
				entityType == net.minecraft.entity.EntityType.WITHER ||
				entityType == net.minecraft.entity.EntityType.ZOMBIE_VILLAGER ||
				entityType == net.minecraft.entity.EntityType.HUSK ||
				entityType == net.minecraft.entity.EntityType.STRAY ||
				entityType == net.minecraft.entity.EntityType.ZOGLIN ||
				entityType == net.minecraft.entity.EntityType.SKELETON_HORSE ||
				entityType == net.minecraft.entity.EntityType.ZOMBIE_HORSE;
			
			// Damage undead heavily, others normally
			if (isUndead) {
				target.damage(caster.getDamageSources().playerAttack(caster), WeaponConfig.MoonSword.MOONFALL_UNDEAD_DAMAGE);
			} else {
				target.damage(caster.getDamageSources().playerAttack(caster), WeaponConfig.MoonSword.MOONFALL_DAMAGE);
			}

			// Light blind all mobs (apply blindness)
			if (target instanceof MobEntity) {
				target.addStatusEffect(new StatusEffectInstance(
					StatusEffects.BLINDNESS,
					WeaponConfig.MoonSword.MOONFALL_BLINDNESS_DURATION_TICKS,
					0,
					false,
					true,
					true
				), caster);
			}
		}

		// Spawn beam visual effect
		spawnMoonfallBeam(world, centerPos);

		// Create lingering glow effect
		activeGlows.put(UUID.randomUUID(), new MoonfallGlow(centerPos, WeaponConfig.MoonSword.MOONFALL_GLOW_DURATION_TICKS, caster.getUuid()));
	}

	/**
	 * Spawns the visual beam effect for Moonfall - shows 3×3 area coverage
	 */
	private void spawnMoonfallBeam(ServerWorld world, BlockPos centerPos) {
		Vector3f moonColor = new Vector3f(0.7f, 0.8f, 1.0f); // White-blue color
		DustParticleEffect moonDust = new DustParticleEffect(moonColor, 1.5f);
		
		// Beam from sky (or high up if underground)
		double beamHeight = Math.min(centerPos.getY() + 20, world.getTopY());
		
		// Spawn beams for each block in the 3×3 area
		for (int xOffset = -1; xOffset <= 1; xOffset++) {
			for (int zOffset = -1; zOffset <= 1; zOffset++) {
				double blockX = centerPos.getX() + xOffset + 0.5;
				double blockY = centerPos.getY() + 0.1;
				double blockZ = centerPos.getZ() + zOffset + 0.5;
				
				// Vertical beam from sky to each block in 3×3 area
				int particleCount = 25;
				for (int i = 0; i < particleCount; i++) {
					double progress = (double) i / (particleCount - 1);
					double y = blockY + (beamHeight - blockY) * (1.0 - progress);
					
					world.spawnParticles(moonDust, 
						blockX + (world.getRandom().nextDouble() - 0.5) * 0.2,
						y,
						blockZ + (world.getRandom().nextDouble() - 0.5) * 0.2,
						1, 0.03, 0.03, 0.03, 0.02);
				}
				
				// Impact particles at each block in the 3×3 area
				world.spawnParticles(moonDust, blockX, blockY, blockZ, 8, 0.3, 0.1, 0.3, 0.08);
				world.spawnParticles(ParticleTypes.END_ROD, blockX, blockY, blockZ, 5, 0.3, 0.1, 0.3, 0.04);
			}
		}
		
		// Additional impact particles in the center for emphasis
		double centerX = centerPos.getX() + 0.5;
		double centerY = centerPos.getY() + 0.1;
		double centerZ = centerPos.getZ() + 0.5;
		world.spawnParticles(ParticleTypes.ENCHANT, centerX, centerY, centerZ, 15, 1.5, 0.1, 1.5, 0.03);
		world.spawnParticles(ParticleTypes.END_ROD, centerX, centerY + 0.5, centerZ, 10, 1.0, 0.2, 1.0, 0.05);
	}

	/**
	 * Called every tick to update lingering glow effects - shows 3×3 area
	 */
	public static void tickMoonfallGlows(ServerWorld world) {
		Iterator<Map.Entry<UUID, MoonfallGlow>> it = activeGlows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, MoonfallGlow> entry = it.next();
			MoonfallGlow glow = entry.getValue();
			
			glow.ticksRemaining--;
			
			// Spawn glow particles across the entire 3×3 area
			if (glow.ticksRemaining > 0) {
				Vector3f moonColor = new Vector3f(0.7f, 0.8f, 1.0f);
				DustParticleEffect moonDust = new DustParticleEffect(moonColor, 1.0f);
				
				// Spawn particles at each block in the 3×3 area to clearly show coverage
				for (int xOffset = -1; xOffset <= 1; xOffset++) {
					for (int zOffset = -1; zOffset <= 1; zOffset++) {
						double blockX = glow.centerPos.getX() + xOffset + 0.5;
						double blockY = glow.centerPos.getY() + 0.1;
						double blockZ = glow.centerPos.getZ() + zOffset + 0.5;
						
						// Multiple particles per block to make the area more visible
						world.spawnParticles(moonDust, 
							blockX, blockY, blockZ, 2, 0.15, 0.05, 0.15, 0.01);
						world.spawnParticles(ParticleTypes.END_ROD, 
							blockX, blockY + 0.2, blockZ, 1, 0.1, 0.05, 0.1, 0.005);
					}
				}
			}
			
			if (glow.ticksRemaining <= 0) {
				it.remove();
			}
		}
	}

	// Clean up when player disconnects
	public static void cleanupPlayer(UUID playerId) {
		activeGlows.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(playerId));
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.moon_sword.passive").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.translatable("item.bookofe.moon_sword.passive.desc").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.bookofe.moon_sword.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.moon_sword.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}

