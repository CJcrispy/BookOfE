package net.cjcrispy.mixin;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.item.ModItems;
import net.cjcrispy.item.custom.BeachBladeItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.util.math.random.Random;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float modifyDamage(float amount, DamageSource source) {
		LivingEntity entity = (LivingEntity) (Object) this;
		
		// Beach Blade: Reduce fire damage for soaked entities
		if (BeachBladeItem.isSoaked(entity.getId()) && source.isOf(DamageTypes.ON_FIRE)) {
			amount *= 0.5f; // 50% fire damage reduction
		}

		// Only apply to players
		if (!(entity instanceof PlayerEntity player)) {
			return amount;
		}

		// Check if player has slime hammer equipped
		ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
		ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
		
		boolean hasSlimeHammer = (mainHand.getItem() == ModItems.SLIME_HAMMER) || 
		                        (offHand.getItem() == ModItems.SLIME_HAMMER);
		
		if (!hasSlimeHammer) {
			return amount;
		}

		// 20% chance to trigger Goo Guard
		Random random = player.getWorld().getRandom();
		if (random.nextDouble() < WeaponConfig.SlimeHammer.GOO_GUARD_CHANCE) {
			// Calculate reduction (20-30%)
			double reductionPercent = WeaponConfig.SlimeHammer.GOO_GUARD_MIN_REDUCTION + 
				random.nextDouble() * (WeaponConfig.SlimeHammer.GOO_GUARD_MAX_REDUCTION - WeaponConfig.SlimeHammer.GOO_GUARD_MIN_REDUCTION);
			
			float reducedAmount = amount * (float) (1.0 - reductionPercent);
			
			// Visual and audio effects
			if (!player.getWorld().isClient() && player.getWorld() instanceof ServerWorld serverWorld) {
				// "Boing" sound
				serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.PLAYERS, 0.8f, 1.2f);
				
				// Tiny slime blobs pop off the player
				for (int i = 0; i < 8; i++) {
					double offsetX = (random.nextDouble() - 0.5) * 0.8;
					double offsetY = random.nextDouble() * 0.5 + 0.5;
					double offsetZ = (random.nextDouble() - 0.5) * 0.8;
					serverWorld.spawnParticles(ParticleTypes.ITEM_SLIME, 
						player.getX() + offsetX, 
						player.getY() + offsetY, 
						player.getZ() + offsetZ, 
						1, 0.1, 0.1, 0.1, 0.05);
				}
			}
			
			return reducedAmount;
		}
		
		return amount;
	}
}

