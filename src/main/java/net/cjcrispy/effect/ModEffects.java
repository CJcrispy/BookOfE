package net.cjcrispy.effect;

import net.cjcrispy.BookOfE;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEffects {

	public static final RegistryEntry<StatusEffect> REND = register("rend", new RendStatusEffect(StatusEffectCategory.HARMFUL, 0x7a0a0a));

	private static RegistryEntry<StatusEffect> register(String id, StatusEffect effect) {
		StatusEffect registered = Registry.register(Registries.STATUS_EFFECT, Identifier.of(BookOfE.MOD_ID, id), effect);
		return Registries.STATUS_EFFECT.getEntry(registered);
	}

	public static void registerModEffects() {
		BookOfE.LOGGER.info("Registering Mod Effects for " + BookOfE.MOD_ID);
	}

	private ModEffects() {}
}


