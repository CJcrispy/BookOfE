package net.cjcrispy.rift;

import net.cjcrispy.config.WeaponConfig;
import net.cjcrispy.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public final class SeveringRiftManager {

	private static final Map<net.minecraft.registry.RegistryKey<World>, List<Rift>> worldRifts = new HashMap<>();

	public static void spawnRift(PlayerEntity player) {
		ServerWorld world = (ServerWorld) player.getWorld();
		var worldKey = world.getRegistryKey();
		Vec3d eye = player.getPos().add(0, player.getStandingEyeHeight(), 0);
		Vec3d dir = player.getRotationVec(1.0f).normalize();
		double half = WeaponConfig.Calamity.RIFT_LENGTH_BLOCKS / 2.0;
		Vec3d center = player.getPos().add(dir.multiply(2.0)); // start a bit in front
		Vec3d tangent = new Vec3d(-dir.z, 0, dir.x).normalize();
		Vec3d start = center.add(tangent.multiply(-half));
		Vec3d end = center.add(tangent.multiply(half));

		Rift rift = new Rift(player.getUuid(), world.getTime(), start, end);
		worldRifts.computeIfAbsent(worldKey, k -> new ArrayList<>()).add(rift);
	}

	public static void tickWorld(ServerWorld world) {
		var worldKey = world.getRegistryKey();
		List<Rift> list = worldRifts.get(worldKey);
		if (list == null || list.isEmpty()) return;
		long time = world.getTime();
		list.removeIf(rift -> {
			// detonation
			if (!rift.detonated && time - rift.spawnTime >= WeaponConfig.Calamity.RIFT_DETONATE_DELAY_TICKS) {
				rift.detonated = true;
				damageEntitiesAlongLine(world, rift, WeaponConfig.Calamity.RIFT_DETONATION_DAMAGE);
			}
			// hazard damage each tick
			if (rift.detonated) {
				damageEntitiesCrossing(world, rift, WeaponConfig.Calamity.RIFT_CROSS_DAMAGE);
			}
			// lifetime
			return time - rift.spawnTime > WeaponConfig.Calamity.RIFT_DETONATE_DELAY_TICKS + WeaponConfig.Calamity.RIFT_PERSIST_TICKS;
		});
	}

	public static void procRendNearby(PlayerEntity player, double radius) {
		if (player.getWorld().isClient()) return;
		ServerWorld world = (ServerWorld) player.getWorld();
		List<Entity> entities = world.getOtherEntities(player, new Box(player.getBlockPos()).expand(radius), e -> e instanceof LivingEntity);
		for (Entity e : entities) {
			LivingEntity le = (LivingEntity) e;
			StatusEffectInstance inst = le.getStatusEffect(ModEffects.REND);
			if (inst != null) {
				int stacks = Math.max(1, inst.getAmplifier() + 1);
				le.damage(world.getDamageSources().playerAttack(player), stacks); // instant bonus
			}
		}
	}

	private static void damageEntitiesAlongLine(ServerWorld world, Rift rift, float amount) {
		List<Entity> entities = world.getOtherEntities(null, rift.getAABB().expand(0.5), e -> e instanceof LivingEntity);
		for (Entity e : entities) {
			if (isNearLine(rift.start, rift.end, e.getPos(), 0.6)) {
				((LivingEntity) e).damage(world.getDamageSources().generic(), amount);
			}
		}
	}

	private static void damageEntitiesCrossing(ServerWorld world, Rift rift, float amount) {
		List<Entity> entities = world.getOtherEntities(null, rift.getAABB().expand(0.5), e -> e instanceof LivingEntity);
		for (Entity e : entities) {
			if (isNearLine(rift.start, rift.end, e.getPos(), 0.45)) {
				((LivingEntity) e).damage(world.getDamageSources().magic(), amount);
			}
		}
	}

	private static boolean isNearLine(Vec3d a, Vec3d b, Vec3d p, double threshold) {
		Vec3d ab = b.subtract(a);
		double t = MathHelper.clamp(p.subtract(a).dotProduct(ab) / ab.lengthSquared(), 0.0, 1.0);
		Vec3d proj = a.add(ab.multiply(t));
		return proj.distanceTo(p) <= threshold;
	}

	private static class Rift {
		private final UUID owner;
		private final long spawnTime;
		private final Vec3d start;
		private final Vec3d end;
		private boolean detonated;
		private Rift(UUID owner, long spawnTime, Vec3d start, Vec3d end) {
			this.owner = owner;
			this.spawnTime = spawnTime;
			this.start = start;
			this.end = end;
			this.detonated = false;
		}
		private Box getAABB() {
			double minX = Math.min(start.x, end.x);
			double minY = Math.min(start.y, end.y) - 1.0;
			double minZ = Math.min(start.z, end.z);
			double maxX = Math.max(start.x, end.x);
			double maxY = Math.max(start.y, end.y) + 2.0;
			double maxZ = Math.max(start.z, end.z);
			return new Box(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}

	private SeveringRiftManager() {}
}


