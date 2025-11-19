package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DustParticleEffect;
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
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public class CalamitySwordItem extends SwordItem {
	public CalamitySwordItem(ToolMaterial toolMaterial, Settings settings) {
		super(toolMaterial, settings);
	}

	/* Calamitous Edge — apply Rend stacks on hit */
	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!target.getWorld().isClient()) {
			StatusEffectInstance current = target.getStatusEffect(ModEffects.REND);
			int maxStacks = WeaponConfig.Calamity.REND_MAX_STACKS;
			int nextAmplifier = 0;
			if (current != null) {
				nextAmplifier = Math.min(maxStacks - 1, current.getAmplifier() + 1);
			}
			target.addStatusEffect(new StatusEffectInstance(
				ModEffects.REND,
				WeaponConfig.Calamity.REND_DURATION_TICKS,
				nextAmplifier,
				false,
				true,
				true
			), attacker);
		}
		return super.postHit(stack, target, attacker);
	}

	/* Cleave — right-click to cleave the entity under crosshair */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		
		// Check cooldown
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}

		if (world.isClient()) {
			return TypedActionResult.success(stack);
		}

		// Raycast to find target entity
		LivingEntity target = findTargetEntity(user, WeaponConfig.Calamity.CLEAVE_RANGE);
		
		if (target != null) {
			// Apply cooldown
			user.getItemCooldownManager().set(this, WeaponConfig.Calamity.CLEAVE_COOLDOWN_TICKS);
			
			// Damage the target
			target.damage(user.getDamageSources().playerAttack(user), WeaponConfig.Calamity.CLEAVE_DAMAGE);
			
			// Spawn red diagonal slash particles
			spawnCleaveParticles((ServerWorld) world, target);
			
			// Sound effect
			world.playSound(null, target.getX(), target.getY(), target.getZ(), 
				SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.8f);
			
			// Trigger instant Rend proc on the cleaved target
			StatusEffectInstance current = target.getStatusEffect(ModEffects.REND);
			if (current != null) {
				// Proc all stored bleed damage instantly
				int stacks = current.getAmplifier() + 1;
				float bleedDamage = stacks * WeaponConfig.Calamity.REND_DAMAGE_PER_TICK * 8; // 8 ticks worth
				target.damage(user.getDamageSources().playerAttack(user), bleedDamage);
			}
			
			return TypedActionResult.success(stack);
		}
		
		return TypedActionResult.fail(stack);
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
		Predicate<Entity> filter = entity -> 
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
	 * Spawns red particles in a diagonal slash pattern across the target
	 */
	private void spawnCleaveParticles(ServerWorld world, LivingEntity target) {
		Vec3d targetPos = target.getPos();
		double centerX = targetPos.x;
		double centerY = targetPos.y + target.getHeight() / 2.0;
		double centerZ = targetPos.z;
		
		// Create a diagonal slash from top-left to bottom-right (relative to player view)
		// The slash goes diagonally across the entity
		double slashLength = Math.max(target.getWidth(), target.getHeight()) * 1.5;
		int particleCount = 30;
		
		// Red color for particles (RGB: 1.0, 0.0, 0.0)
		Vector3f redColor = new Vector3f(1.0f, 0.0f, 0.0f);
		DustParticleEffect redDust = new DustParticleEffect(redColor, 1.0f);
		
		// Diagonal slash direction (45 degrees)
		// We'll create a slash that goes from one corner to the opposite
		for (int i = 0; i < particleCount; i++) {
			double progress = (double) i / (particleCount - 1);
			
			// Diagonal from top-left to bottom-right
			double offsetX = (progress - 0.5) * slashLength * 0.7;
			double offsetY = (0.5 - progress) * slashLength * 0.7; // Goes from top to bottom
			double offsetZ = (progress - 0.5) * slashLength * 0.7;
			
			double x = centerX + offsetX;
			double y = centerY + offsetY;
			double z = centerZ + offsetZ;
			
			// Spawn red dust particles
			world.spawnParticles(redDust, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
			
			// Also add some crit particles for extra effect
			if (i % 3 == 0) {
				world.spawnParticles(ParticleTypes.CRIT, x, y, z, 1, 0.1, 0.1, 0.1, 0.1);
			}
		}
		
		// Add some extra particles at the impact points
		world.spawnParticles(redDust, centerX, centerY, centerZ, 10, 0.3, 0.3, 0.3, 0.1);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.calamityblade.passive").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.translatable("item.bookofe.calamityblade.passive.desc").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.bookofe.calamityblade.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.calamityblade.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}


