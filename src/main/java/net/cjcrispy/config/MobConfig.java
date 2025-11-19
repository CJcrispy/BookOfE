package net.cjcrispy.config;

public final class MobConfig {

	// Helper method to generate randomized stats based on seed
	// Uses a simple hash-based approach for deterministic randomness without creating Random instances
	private static double randomizeStat(long seed, double min, double max) {
		// Use a simple hash-based pseudo-random function for deterministic results
		long hash = seed;
		hash = hash * 0x9e3779b9 + 0x517cc1b7;
		hash = hash ^ (hash >>> 16);
		hash = hash * 0x9e3779b9;
		hash = hash ^ (hash >>> 16);
		// Convert to double in [0, 1) range
		double normalized = ((hash & 0xFFFFFFFFL) / 4294967296.0);
		return min + (max - min) * normalized;
	}

	public static final class ShadowQuinn {
		// Tier 2 Boss - Mid-high difficulty (below KingHajile, above tier 3)
		private static final long SEED = "ShadowQuinn".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 250.0, 400.0);
		public static final double MOVEMENT_SPEED = 0.30;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 15.0, 25.0);
		public static final double ATTACK_KNOCKBACK = 2.0;
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 8.0, 18.0);
		public static final double KNOCKBACK_RESISTANCE = 0.8; // 80% knockback resistance
		public static final double SCALE = 1.35;
	}

	public static final class DarkWizard {
		public static final double MAX_HEALTH = 50.0;
		public static final double MOVEMENT_SPEED = 0.35;
		public static final double ATTACK_DAMAGE = 1.0;
		public static final double FOLLOW_RANGE = 10.0;
	}

	public static final class BlackBird {
		public static final double MAX_HEALTH = 10.0;
		public static final double ARMOR = 0.0;
		public static final double MOVEMENT_SPEED = 0.35;
		public static final double ATTACK_DAMAGE = 5.0;
		public static final double FOLLOW_RANGE = 10.0;
		public static final double SCALE = 1.35;
	}

	public static final class MillyKnight {
		// Tier 3 Boss - Lower difficulty (easiest tier)
		private static final long SEED = "MillyKnight".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 200.0, 350.0);
		public static final double MOVEMENT_SPEED = 0.30;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 12.0, 22.0);
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 5.0, 15.0);
		public static final double KNOCKBACK_RESISTANCE = 6.0;
		public static final double SCALE = 1.35;
	}


	public static final class NickySummoner {
		// Tier 3 Boss - Lower difficulty (easiest tier)
		private static final long SEED = "NickySummoner".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 200.0, 350.0);
		public static final double MOVEMENT_SPEED = 0.30;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 12.0, 22.0);
		public static final double ATTACK_KNOCKBACK = 2.0;
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 5.0, 15.0);
		public static final double SCALE = 1.35;
	}

	public static final class JoeRebel {
		// Tier 3 Boss - Lower difficulty (easiest tier)
		private static final long SEED = "JoeRebel".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 200.0, 350.0);
		public static final double MOVEMENT_SPEED = 0.45;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 12.0, 22.0);
		public static final double ATTACK_KNOCKBACK = 2.0;
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 5.0, 15.0);
		public static final double SCALE = 1.35;
	}

	public static final class KingHajile {
		// Tier 1 Boss - Final Boss (highest difficulty)
		private static final long SEED = "KingHajile".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 350.0, 500.0);
		public static final double MOVEMENT_SPEED = 0.30;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 20.0, 30.0);
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 10.0, 20.0);
		public static final double KNOCKBACK_RESISTANCE = 6.0;
		public static final double SCALE = 1.35;
	}

	public static final class SlimeChris {
		// Tier 2 Boss - Mid-high difficulty (below KingHajile, above tier 3)
		private static final long SEED = "SlimeChris".hashCode();
		public static final double MAX_HEALTH = randomizeStat(SEED, 250.0, 400.0);
		public static final double MOVEMENT_SPEED = 0.30;
		public static final double ATTACK_DAMAGE = randomizeStat(SEED + 1, 15.0, 25.0);
		public static final double FOLLOW_RANGE = 30.0;
		public static final double ARMOR = randomizeStat(SEED + 2, 8.0, 18.0);
		public static final double KNOCKBACK_RESISTANCE = 6.0;
		public static final double SCALE = 1.35;
	}

	public static final class SlimeMinion {
		public static final double MAX_HEALTH = 1.0;
		public static final double MOVEMENT_SPEED = 0.25;
		public static final double ATTACK_DAMAGE = 2.0;
		public static final double FOLLOW_RANGE = 16.0;
		public static final double SCALE = 1.35;
	}

	private MobConfig() {}
}

