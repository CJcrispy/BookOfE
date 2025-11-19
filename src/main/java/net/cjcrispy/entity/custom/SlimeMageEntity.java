package net.cjcrispy.entity.custom;

import net.cjcrispy.config.MobConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

public class SlimeMageEntity extends HostileEntity implements GeoEntity {
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int attackAnimationTimer = 0;
	private int spellCooldown = 0;
	private java.util.UUID ownerUuid = null;

	public SlimeMageEntity(EntityType<? extends HostileEntity> entityType, World world) {
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
				// Don't target the owner
				if (living instanceof PlayerEntity player && ownerUuid != null && player.getUuid().equals(ownerUuid)) {
					return false;
				}
				// Don't target other slime minions
				return !(living instanceof PlayerEntity) && 
				       !(living instanceof SlimeCommonEntity) &&
				       !(living instanceof SlimeMageEntity) &&
				       !(living instanceof SlimeWarriorEntity);
			}));
		this.targetSelector.add(2, new RevengeGoal(this) {
			@Override
			public boolean canStart() {
				// Don't revenge against owner
				if (mob.getAttacker() instanceof PlayerEntity player && ownerUuid != null && player.getUuid().equals(ownerUuid)) {
					return false;
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
		} else {
			// Apply slow effect to enemies on hit
			if (spellCooldown > 0) {
				spellCooldown--;
			}
			
			LivingEntity target = this.getTarget();
			if (target != null && this.squaredDistanceTo(target) < 16.0 && spellCooldown <= 0) {
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0));
				spellCooldown = 40; // 2 second cooldown
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

