package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.Item;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class BlackbornSwordItem extends SwordItem {
	private static final double TRUE_DAMAGE_CHANCE = WeaponConfig.Blackborn.TRUE_DAMAGE_CHANCE; // 60%
	private static final float TRUE_DAMAGE_AMOUNT = WeaponConfig.Blackborn.TRUE_DAMAGE_AMOUNT; // bonus true damage (armor-piercing)
	private static final int WITHER_TICKS = WeaponConfig.Blackborn.WITHER_TICKS; // 3 seconds

	private static final Random RANDOM = new Random();

	public BlackbornSwordItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!target.getWorld().isClient()) {
			if (RANDOM.nextDouble() < TRUE_DAMAGE_CHANCE) {
				float newHealth = Math.max(0.0f, target.getHealth() - TRUE_DAMAGE_AMOUNT);
				target.setHealth(newHealth);
				target.timeUntilRegen = 0;
				target.playSound(SoundEvents.ENTITY_WITHER_HURT, 0.6f, 0.8f + RANDOM.nextFloat() * 0.4f);
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_TICKS, 0), attacker);
			}
		}
		return super.postHit(stack, target, attacker);
	}

	/**
	 * Active: Void Slash - Right-click to unleash a dark energy wave
	 */
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

		// Apply cooldown
		user.getItemCooldownManager().set(this, WeaponConfig.Blackborn.VOID_SLASH_COOLDOWN_TICKS);

		// Execute void slash
		executeVoidSlash((ServerWorld) world, user);

		// Sound effect
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
			SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.6f);

		return TypedActionResult.success(stack, false);
	}

	/**
	 * Executes the Void Slash ability - creates a dark energy wave in front of the player
	 */
	private void executeVoidSlash(ServerWorld world, PlayerEntity caster) {
		Vec3d eyePos = caster.getEyePos();
		Vec3d lookVec = caster.getRotationVector();
		Vec3d forward = lookVec.normalize();

		// Calculate the slash area (cone/wave in front of player)
		double range = WeaponConfig.Blackborn.VOID_SLASH_RANGE;
		double width = WeaponConfig.Blackborn.VOID_SLASH_WIDTH;

		// Find all entities in the slash area
		Vec3d endPos = eyePos.add(forward.multiply(range));
		Box searchBox = new Box(eyePos, endPos).expand(width);

		Predicate<Entity> filter = entity ->
			entity instanceof LivingEntity
			&& entity.isAttackable()
			&& entity != caster
			&& !entity.isSpectator();

		List<Entity> entities = world.getOtherEntities(caster, searchBox, filter);

		// Damage entities in the slash path
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity living)) continue;

			// Check if entity is within the slash cone
			Vec3d toEntity = living.getPos().subtract(eyePos);
			double distance = toEntity.length();
			if (distance > range) continue;

			// Project entity position onto the forward direction
			double projection = toEntity.dotProduct(forward);
			if (projection < 0) continue; // Behind player

			// Calculate perpendicular distance from the center line
			Vec3d perpendicular = toEntity.subtract(forward.multiply(projection));
			double perpDistance = perpendicular.length();

			// Check if within width (cone expands with distance)
			double maxWidth = width * (projection / range);
			if (perpDistance > maxWidth) continue;

			// Damage the entity
			living.damage(caster.getDamageSources().playerAttack(caster), WeaponConfig.Blackborn.VOID_SLASH_DAMAGE);

			// Apply wither effect
			living.addStatusEffect(new StatusEffectInstance(
				StatusEffects.WITHER,
				WeaponConfig.Blackborn.VOID_SLASH_WITHER_TICKS,
				1, // Amplifier 1 = stronger wither
				false,
				true,
				true
			), caster);

			// Spawn dark particles on hit
			spawnVoidHitParticles(world, living);
		}

		// Spawn the void slash visual effect
		spawnVoidSlashEffect(world, eyePos, forward, range, width);
	}

	/**
	 * Spawns the visual void slash wave effect
	 */
	private void spawnVoidSlashEffect(ServerWorld world, Vec3d startPos, Vec3d direction, double range, double width) {
		Vector3f voidColor = new Vector3f(0.1f, 0.0f, 0.2f); // Dark purple/black
		DustParticleEffect voidDust = new DustParticleEffect(voidColor, 1.5f);

		// Create a wave/slash effect
		int particleCount = 50;
		for (int i = 0; i < particleCount; i++) {
			double progress = (double) i / (particleCount - 1);
			double distance = progress * range;

			// Create a curved slash pattern
			double angle = progress * Math.PI * 2; // Full rotation for wave effect
			double horizontalOffset = Math.cos(angle) * width * (1 - progress * 0.5);
			double verticalOffset = Math.sin(progress * Math.PI) * 0.5; // Arc motion
			double depthOffset = Math.sin(angle) * width * (1 - progress * 0.5);

			Vec3d forward = direction.multiply(distance);
			Vec3d right = new Vec3d(-direction.z, 0, direction.x).normalize();
			Vec3d up = direction.crossProduct(right).normalize();

			Vec3d particlePos = startPos
				.add(forward)
				.add(right.multiply(horizontalOffset))
				.add(up.multiply(verticalOffset))
				.add(direction.multiply(depthOffset * 0.3)); // Add depth for 3D effect

			world.spawnParticles(voidDust,
				particlePos.x, particlePos.y, particlePos.z,
				1, 0.1, 0.1, 0.1, 0.02);

			// Add some end rod particles for glow
			if (i % 5 == 0) {
				world.spawnParticles(ParticleTypes.END_ROD,
					particlePos.x, particlePos.y, particlePos.z,
					1, 0.15, 0.15, 0.15, 0.01);
			}
		}

		// Add a dark explosion effect at the end
		Vec3d endPos = startPos.add(direction.multiply(range));
		world.spawnParticles(voidDust, endPos.x, endPos.y, endPos.z, 20, width, 1.0, width, 0.1);
		world.spawnParticles(ParticleTypes.SMOKE, endPos.x, endPos.y, endPos.z, 15, width * 0.5, 0.5, width * 0.5, 0.05);
	}

	/**
	 * Spawns dark particles when an entity is hit by void slash
	 */
	private void spawnVoidHitParticles(ServerWorld world, LivingEntity target) {
		Vec3d pos = target.getPos();
		Vector3f voidColor = new Vector3f(0.1f, 0.0f, 0.2f);
		DustParticleEffect voidDust = new DustParticleEffect(voidColor, 1.0f);

		world.spawnParticles(voidDust, pos.x, pos.y + target.getHeight() / 2.0, pos.z, 10, 0.3, 0.3, 0.3, 0.05);
		world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + target.getHeight() / 2.0, pos.z, 5, 0.2, 0.2, 0.2, 0.03);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.passive").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.passive.desc").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.ability").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.ability.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}


