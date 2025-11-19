package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SlimeCommonEntity extends HostileEntity implements GeoEntity {
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int attackAnimationTimer = 0;
	private java.util.UUID ownerUuid = null;

	public SlimeCommonEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 2;
	}

	@Override
	protected void initGoals() {
		// Attack-related goals
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
		this.targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false, 
			entity -> {
				if (!(entity instanceof LivingEntity living)) return false;
				// Don't target the owner (boss)
				if (ownerUuid != null && living.getUuid().equals(ownerUuid)) {
					return false;
				}
				// Don't target other slime minions
				if (living instanceof SlimeCommonEntity ||
				    living instanceof SlimeMageEntity ||
				    living instanceof SlimeWarriorEntity) {
					return false;
				}
				// Only target entities that have attacked the boss
				if (ownerUuid != null && !this.getWorld().isClient() && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
					// Find the boss entity
					net.minecraft.util.math.Box searchBox = this.getBoundingBox().expand(100.0);
					for (net.cjcrispy.entity.custom.SlimeChrisEntity boss : serverWorld.getEntitiesByClass(
							net.cjcrispy.entity.custom.SlimeChrisEntity.class, searchBox,
							boss -> boss.getUuid().equals(ownerUuid))) {
						return boss.getAttackers().contains(living.getUuid());
					}
				}
				return false; // Don't target anything else
			}));
		this.targetSelector.add(2, new RevengeGoal(this) {
			@Override
			public boolean canStart() {
				// Don't revenge against owner
				if (mob.getAttacker() != null && ownerUuid != null && mob.getAttacker().getUuid().equals(ownerUuid)) {
					return false;
				}
				// Only revenge if the attacker also attacked the boss
				if (mob.getAttacker() != null && ownerUuid != null && !mob.getWorld().isClient() && mob.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
					// Find the boss entity
					net.minecraft.util.math.Box searchBox = mob.getBoundingBox().expand(100.0);
					for (net.cjcrispy.entity.custom.SlimeChrisEntity boss : serverWorld.getEntitiesByClass(
							net.cjcrispy.entity.custom.SlimeChrisEntity.class, searchBox,
							boss -> boss.getUuid().equals(ownerUuid))) {
						return boss.getAttackers().contains(mob.getAttacker().getUuid());
					}
				}
				return super.canStart();
			}
		});

		// General AI behavior goals
		this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(6, new LookAroundGoal(this));
		this.goalSelector.add(7, new SwimGoal(this));
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, MobConfig.SlimeMinion.MAX_HEALTH)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MobConfig.SlimeMinion.MOVEMENT_SPEED)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, MobConfig.SlimeMinion.ATTACK_DAMAGE)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, MobConfig.SlimeMinion.FOLLOW_RANGE)
			.add(EntityAttributes.GENERIC_SCALE, MobConfig.SlimeMinion.SCALE);
	}

	public void setOwnerUuid(java.util.UUID uuid) {
		this.ownerUuid = uuid;
	}

	public java.util.UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (ownerUuid != null) {
			nbt.putUuid("OwnerUuid", ownerUuid);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("OwnerUuid")) {
			ownerUuid = nbt.getUuid("OwnerUuid");
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient()) {
			if (attackAnimationTimer > 0) {
				attackAnimationTimer--;
			}
		}
	}

	@Override
	public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		if (this.isDead()) {
			tAnimationState.getController().setAnimation(RawAnimation.begin().then("death", Animation.LoopType.PLAY_ONCE));
			return PlayState.STOP;
		}

		if (attackAnimationTimer > 0) {
			tAnimationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
			return PlayState.CONTINUE;
		}

		if (this.handSwinging) {
			tAnimationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
			attackAnimationTimer = 20;
			return PlayState.CONTINUE;
		}

		if (tAnimationState.isMoving()) {
			tAnimationState.getController().setAnimation(
				RawAnimation.begin().then("walk", Animation.LoopType.LOOP)
			);
			return PlayState.CONTINUE;
		} else {
			tAnimationState.getController().setAnimation(
				RawAnimation.begin().then("idle", Animation.LoopType.LOOP)
			);
			return PlayState.CONTINUE;
		}
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}

