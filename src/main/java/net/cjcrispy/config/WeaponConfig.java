package net.cjcrispy.config;

import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

public final class WeaponConfig {

	// Helper method to generate randomized stats based on seed
	// Uses a simple hash-based approach for deterministic randomness without creating Random instances
	private static int randomizeIntStat(long seed, int min, int max) {
		long hash = seed;
		hash = hash * 0x9e3779b9 + 0x517cc1b7;
		hash = hash ^ (hash >>> 16);
		hash = hash * 0x9e3779b9;
		hash = hash ^ (hash >>> 16);
		// Convert to int in [min, max] range
		int range = max - min + 1;
		return min + (int) ((hash & 0x7FFFFFFFL) % range);
	}

	private static float randomizeFloatStat(long seed, float min, float max) {
		long hash = seed;
		hash = hash * 0x9e3779b9 + 0x517cc1b7;
		hash = hash ^ (hash >>> 16);
		hash = hash * 0x9e3779b9;
		hash = hash ^ (hash >>> 16);
		// Convert to float in [0, 1) range
		float normalized = ((hash & 0xFFFFFFFFL) / 4294967296.0f);
		return min + (max - min) * normalized;
	}

	public static final class WormHammer {
		private static final long SEED = "WormHammer".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5); // used with ToolMaterial base
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);
		public static final int BURROW_SLAM_COOLDOWN_TICKS = 240; // 12s
	}

	public static final class SlimeHammer {
		private static final long SEED = "SlimeHammer".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5);
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);

		// Goo Guard passive
		public static final double GOO_GUARD_CHANCE = 0.20; // 20% chance
		public static final double GOO_GUARD_MIN_REDUCTION = 0.20; // 20% reduction
		public static final double GOO_GUARD_MAX_REDUCTION = 0.30; // 30% reduction

		// Slime Split ability
		public static final int SLIME_SPLIT_COOLDOWN_TICKS = 240; // 12 seconds
		public static final int SLIME_SPLIT_DURATION_TICKS = 100; // 5 seconds
	}

	public static final class MoonSword {
		private static final long SEED = "MoonSword".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5);
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);

		// Lunar Edge passive
		public static final float LUNAR_EDGE_DAMAGE_MULTIPLIER = 1.2f; // +20% damage

		// Moonfall ability
		public static final int MOONFALL_COOLDOWN_TICKS = 240; // 12 seconds
		public static final double MOONFALL_RANGE = 20.0; // blocks
		public static final float MOONFALL_DAMAGE = 10.0f; // base damage
		public static final float MOONFALL_UNDEAD_DAMAGE = 16.0f; // damage to undead
		public static final int MOONFALL_BLINDNESS_DURATION_TICKS = 60; // 3 seconds
		public static final int MOONFALL_GLOW_DURATION_TICKS = 40; // 2 seconds
	}

	public static final class BeachBlade {
		private static final long SEED = "BeachBlade".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5);
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);

		// Riptide Crash ability
		public static final int RIPTIDE_CRASH_COOLDOWN_TICKS = 200; // 10 seconds
		public static final double RIPTIDE_CRASH_DISTANCE = 7.0; // average 7 blocks (6-8 range)
		public static final double RIPTIDE_CRASH_SPEED = 1.8; // dash speed
		public static final float RIPTIDE_CRASH_DAMAGE = 8.0f; // bonus damage to first hit
		public static final int RIPTIDE_CRASH_DOLPHIN_GRACE_TICKS = 20; // 1 second
		public static final int RIPTIDE_CRASH_SOAKED_DURATION_TICKS = 100; // 5 seconds
		public static final double RIPTIDE_CRASH_HIT_RANGE = 1.5; // hit detection range
		public static final double RIPTIDE_CRASH_FIRE_EXTINGUISH_RANGE = 1.0; // range to extinguish fire/lava
	}

	public static final class Blackborn {
		private static final long SEED = "Blackborn".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5);
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);

		public static final double TRUE_DAMAGE_CHANCE = 0.60; // 60%
		public static final float TRUE_DAMAGE_AMOUNT = 3.0f;
		public static final int WITHER_TICKS = 60; // 3 seconds

		// Void Slash ability
		public static final int VOID_SLASH_COOLDOWN_TICKS = 200; // 10 seconds
		public static final double VOID_SLASH_RANGE = 12.0; // blocks
		public static final double VOID_SLASH_WIDTH = 5.0; // width of the slash
		public static final float VOID_SLASH_DAMAGE = 10.0f; // base damage
		public static final int VOID_SLASH_WITHER_TICKS = 100; // 5 seconds of wither
	}

	public static final class Calamity {
		private static final long SEED = "Calamity".hashCode();
		public static final ToolMaterial MATERIAL = ToolMaterials.NETHERITE;
		public static final int ADDITIONAL_DAMAGE = randomizeIntStat(SEED, 1, 5);
		public static final float ATTACK_SPEED = randomizeFloatStat(SEED + 1, -2.8f, -1.0f);

		// Calamitous Edge (Rend) passive
		public static final int REND_MAX_STACKS = 5; // 3–5; using 5
		public static final int REND_DURATION_TICKS = 80; // 4s
		public static final int REND_TICK_INTERVAL_TICKS = 10; // 0.5s
		public static final float REND_DAMAGE_PER_TICK = 0.5f; // per stack; small damage

		// Cleave ability
		public static final int CLEAVE_COOLDOWN_TICKS = 80; // 4s
		public static final double CLEAVE_RANGE = 20.0; // blocks
		public static final float CLEAVE_DAMAGE = 12.0f; // base cleave damage

		// Legacy Severing Rift (kept for SeveringRiftManager compatibility)
		public static final int RIFT_COOLDOWN_TICKS = 360; // 18s
		public static final int RIFT_CHARGE_TICKS = 20; // 1s to charge
		public static final double RIFT_LENGTH_BLOCKS = 8.0; // line length
		public static final int RIFT_DETONATE_DELAY_TICKS = 20; // erupts after 1s
		public static final int RIFT_PERSIST_TICKS = 60; // hazard persists 3s
		public static final float RIFT_DETONATION_DAMAGE = 8.0f; // slicing burst
		public static final float RIFT_CROSS_DAMAGE = 6.0f; // crossing line while active
	}

	private WeaponConfig() {}
}

