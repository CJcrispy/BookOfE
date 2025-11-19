package net.cjcrispy.item.custom;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.List;

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

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.passive").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.translatable("item.bookofe.the_black_blade.passive.desc").formatted(Formatting.GRAY));
		super.appendTooltip(stack, context, tooltip, type);
	}
}


