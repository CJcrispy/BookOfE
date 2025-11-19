package net.cjcrispy.effect;

import net.cjcrispy.config.WeaponConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;

public class RendStatusEffect extends StatusEffect {
	public RendStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = Math.max(1, WeaponConfig.Calamity.REND_TICK_INTERVAL_TICKS);
		return duration % interval == 0;
	}

	@Override
	public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (entity.getWorld().isClient()) return false;
		float perStack = WeaponConfig.Calamity.REND_DAMAGE_PER_TICK;
		int stacks = Math.max(1, amplifier + 1);
		float damage = perStack * stacks;
		ServerWorld serverWorld = (ServerWorld) entity.getWorld();
		entity.damage(serverWorld.getDamageSources().generic(), damage);
		return true;
	}
}


