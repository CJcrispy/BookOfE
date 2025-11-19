package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.ModEntities;
import net.cjcrispy.entity.custom.JoeRebelCloneEntity;
import net.cjcrispy.entity.custom.JoeRebelEntity;
import net.cjcrispy.entity.projectile.ThrowingKnifeEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class JoeRebelGoal extends Goal {
    protected final JoeRebelEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private long lastUpdateTime;
    
    // Ability cooldowns
    private int dashCooldown = 0;
    private int throwingKnivesCooldown = 0;
    private int smokeBombCooldown = 0;
    private int shadowCloneCooldown = 0;
    private int arrowRainCooldown = 0;
    
    // Ability timers
    private int dashHitTimer = 0;
    private int throwingKnivesTimer = 0;
    private int smokeBombInvisibilityTimer = 0;
    private int smokeBombDashTimer = 0;
    private int nightfallChargeTimer = 0;
    private int nightfallDashTimer = 0;
    
    // Constants
    private static final int DASH_COOLDOWN = 100; // 5 seconds
    private static final int THROWING_KNIVES_COOLDOWN = 80; // 4 seconds
    private static final int SMOKE_BOMB_COOLDOWN = 200; // 10 seconds
    private static final int SHADOW_CLONE_COOLDOWN = 150; // 7.5 seconds
    private static final int ARROW_RAIN_COOLDOWN = 180; // 9 seconds
    
    private static final int DASH_HIT_DELAY = 10; // Hit at 0.5 seconds into dash
    private static final int SMOKE_BOMB_INVISIBILITY_DURATION = 40; // 2 seconds
    private static final int NIGHTFALL_CHARGE_DURATION = 40; // 2 seconds charge
    private static final int NIGHTFALL_DASH_DURATION = 20; // 1 second dash

    public JoeRebelGoal(JoeRebelEntity mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speed = speed;
        this.pauseWhenMobIdle = pauseWhenMobIdle;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        long currentTime = this.mob.getWorld().getTime();
        if (currentTime - this.lastUpdateTime < ATTACK_INTERVAL_TICKS) {
            return false;
        } else {
            this.lastUpdateTime = currentTime;
            LivingEntity livingEntity = this.mob.getTarget();
            if (livingEntity == null || !livingEntity.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().findPathTo(livingEntity, 0);
                return this.path != null || this.mob.isInAttackRange(livingEntity);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) {
            return false;
        } else if (!this.pauseWhenMobIdle) {
            return !this.mob.getNavigation().isIdle();
        } else {
            return this.mob.isInWalkTargetRange(livingEntity.getBlockPos());
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().startMovingAlong(this.path, this.speed);
        this.mob.setAttacking(true);
        this.updateCountdownTicks = 0;
        this.cooldown = 0;
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            this.mob.setTarget(null);
        }
        this.mob.setAttacking(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null) return;
        
        this.mob.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
        this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);
        
        // Update cooldowns
        if (dashCooldown > 0) dashCooldown--;
        if (throwingKnivesCooldown > 0) throwingKnivesCooldown--;
        if (smokeBombCooldown > 0) smokeBombCooldown--;
        if (shadowCloneCooldown > 0) shadowCloneCooldown--;
        if (arrowRainCooldown > 0) arrowRainCooldown--;

        // Update pathfinding
        if ((this.pauseWhenMobIdle || this.mob.getVisibilityCache().canSee(livingEntity)) && this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = 4 + this.mob.getRandom().nextInt(7);
            double d = this.mob.squaredDistanceTo(livingEntity);
            if (d > 1024.0F) {
                this.updateCountdownTicks += 10;
            } else if (d > 256.0F) {
                this.updateCountdownTicks += 5;
            }

            if (!this.mob.getNavigation().startMovingTo(livingEntity, this.speed)) {
                this.updateCountdownTicks += 15;
            }

            this.updateCountdownTicks = this.getTickCount(this.updateCountdownTicks);
        }

        this.cooldown = Math.max(this.cooldown - 1, 0);
        
        // Handle ability timers
        handleDashTimer(livingEntity);
        handleThrowingKnivesTimer(livingEntity);
        handleSmokeBombTimers(livingEntity);
        handleNightfallTimers(livingEntity);
        
        // Stop navigation during attack animations (except dash and nightfall)
        String currentAttackState = this.mob.getAttackState();
        if (!currentAttackState.isEmpty() && !"dash".equals(currentAttackState) && !"nightfall".equals(currentAttackState)) {
            this.mob.getNavigation().stop();
        }
        
        // Try to use an ability or attack
        this.attack(livingEntity);
    }
    
    private void handleDashTimer(LivingEntity target) {
        if (dashHitTimer > 0) {
            dashHitTimer--;
            if (dashHitTimer == 0 && "dash".equals(mob.getAttackState())) {
                performDashHit(target);
            }
        }
    }
    
    private void handleThrowingKnivesTimer(LivingEntity target) {
        if (throwingKnivesTimer > 0) {
            throwingKnivesTimer--;
            if (throwingKnivesTimer > 0 && throwingKnivesTimer % 5 == 0 && "throwing_knives".equals(mob.getAttackState())) {
                // Throw knives in sequence
                int knivesThrown = (20 - throwingKnivesTimer) / 5;
                if (knivesThrown < 3) {
                    throwKnife(target, knivesThrown);
                }
            }
        }
    }
    
    private void handleSmokeBombTimers(LivingEntity target) {
        if (smokeBombInvisibilityTimer > 0) {
            smokeBombInvisibilityTimer--;
            if (smokeBombInvisibilityTimer == 0) {
                // Invisibility ended, prepare dash strike
                smokeBombDashTimer = 5; // Small delay before dash
            }
        }
        
        if (smokeBombDashTimer > 0) {
            smokeBombDashTimer--;
            if (smokeBombDashTimer == 0) {
                performSmokeBombDashStrike(target);
            }
        }
    }
    
    private void handleNightfallTimers(LivingEntity target) {
        if (nightfallChargeTimer > 0) {
            nightfallChargeTimer--;
            if (nightfallChargeTimer == 0) {
                // Start the dash
                nightfallDashTimer = NIGHTFALL_DASH_DURATION;
                performNightfallDash(target);
            }
        }
        
        if (nightfallDashTimer > 0) {
            nightfallDashTimer--;
            if (nightfallDashTimer == 0) {
                // Dash completed, check if hit
                checkNightfallHit(target);
            }
        }
    }

    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();
            
            // Check for Nightfall Execution (only once, below 20% HP)
            if (!mob.hasUsedNightfallExecution() && mob.getHealth() <= mob.getMaxHealth() * 0.20) {
                performNightfallExecution(target);
                return;
            }
            
            // Choose ability based on conditions
            String ability = this.chooseAbility(target);
            
            switch (ability) {
                case "AssassinsDash":
                    performAssassinsDash(target);
                    break;
                case "ThrowingKnives":
                    performThrowingKnives(target);
                    break;
                case "SmokeBomb":
                    performSmokeBomb(target);
                    break;
                case "ShadowClone":
                    performShadowClone(target);
                    break;
                case "ArrowRain":
                    performArrowRain(target);
                    break;
                case "Melee":
                    performMeleeAttack(target);
                    break;
            }
        }
    }
    
    private String chooseAbility(LivingEntity target) {
        double distanceSq = this.mob.squaredDistanceTo(target);
        double random = this.mob.getRandom().nextDouble();
        
        // Close range abilities
        if (distanceSq <= 16.0) {
            if (dashCooldown <= 0 && random < 0.3) {
                return "AssassinsDash";
            }
            if (shadowCloneCooldown <= 0 && random < 0.25) {
                return "ShadowClone";
            }
        }
        
        // Medium range abilities
        if (distanceSq <= 64.0 && distanceSq > 16.0) {
            if (throwingKnivesCooldown <= 0 && random < 0.3) {
                return "ThrowingKnives";
            }
            if (smokeBombCooldown <= 0 && random < 0.3) {
                return "SmokeBomb";
            }
            if (arrowRainCooldown <= 0 && random < 0.25) {
                return "ArrowRain";
            }
        }
        
        // Long range - prefer throwing knives and arrow rain
        if (distanceSq > 64.0) {
            if (arrowRainCooldown <= 0 && random < 0.4) {
                return "ArrowRain";
            }
            if (throwingKnivesCooldown <= 0 && random < 0.4) {
                return "ThrowingKnives";
            }
        }
        
        // Default to melee
        return "Melee";
    }
    
    // Ability 1: Assassin's Dash
    private void performAssassinsDash(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("dash");
        dashCooldown = DASH_COOLDOWN;
        dashHitTimer = DASH_HIT_DELAY;
        
        // Crouch telegraph (visual only - handled by animation)
        // Dash through player
        Vec3d direction = target.getPos().subtract(this.mob.getPos()).normalize();
        Vec3d dashTarget = target.getPos().add(direction.multiply(3.0)); // Dash 3 blocks past player
        
        // Move entity
        this.mob.setPosition(dashTarget.x, this.mob.getY(), dashTarget.z);
        this.mob.setVelocity(direction.multiply(1.5));
        
        // Spawn shadow trail particles
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 10; i++) {
                double t = i / 10.0;
                double x = this.mob.getX() + (dashTarget.x - this.mob.getX()) * t;
                double z = this.mob.getZ() + (dashTarget.z - this.mob.getZ()) * t;
                serverWorld.spawnParticles(ParticleTypes.SMOKE, x, this.mob.getY() + 0.5, z, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }
    
    private void performDashHit(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        if (!this.mob.isInAttackRange(target)) return;
        
        // Deal light damage
        float damage = (float) (this.mob.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) * 0.5);
        target.damage(this.mob.getDamageSources().mobAttack(this.mob), damage);
        
        // Apply blindness for 0.5 seconds (10 ticks)
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10, 0, false, false));
        
        // Teleport behind player
        Vec3d playerPos = target.getPos();
        Vec3d playerLook = target.getRotationVector();
        Vec3d behindPos = playerPos.subtract(playerLook.multiply(2.0));
        this.mob.setPosition(behindPos.x, this.mob.getY(), behindPos.z);
        
        // Face the player
        this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
    }
    
    // Ability 2: Throwing Knives
    private void performThrowingKnives(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("throwing_knives");
        throwingKnivesCooldown = THROWING_KNIVES_COOLDOWN;
        throwingKnivesTimer = 20; // Throw 3 knives over 1 second
        
        // Throw first knife immediately
        throwKnife(target, 0);
    }
    
    private void throwKnife(LivingEntity target, int index) {
        if (this.mob.getWorld().isClient()) return;
        
        double targetX = target.getX() - this.mob.getX();
        double targetY = target.getBodyY(0.33D) - this.mob.getBodyY(0.33D);
        double targetZ = target.getZ() - this.mob.getZ();
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        
        // Adjust for fan pattern if in Rebellion's Fury
        if (mob.isRebellionFuryActive() && index > 0) {
            double angle = (index - 1.5) * 0.2; // Spread knives
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double newX = targetX * cos - targetZ * sin;
            double newZ = targetX * sin + targetZ * cos;
            targetX = newX;
            targetZ = newZ;
        }
        
        ThrowingKnifeEntity knife = new ThrowingKnifeEntity(
            this.mob.getWorld(),
            this.mob,
            this.mob.getX(),
            this.mob.getBodyY(0.5D),
            this.mob.getZ()
        );
        
        float velocity = mob.isRebellionFuryActive() ? 2.0f : 1.5f;
        knife.setVelocity(targetX, targetY + horizontalDistance * 0.2D, targetZ, velocity, 0.8F);
        
        this.mob.playSound(SoundEvents.ITEM_TRIDENT_THROW.value(), 1.0F, 1.2F + this.mob.getRandom().nextFloat() * 0.2F);
        this.mob.getWorld().spawnEntity(knife);
    }
    
    // Ability 3: Smoke Bomb
    private void performSmokeBomb(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("smoke_bomb");
        smokeBombCooldown = SMOKE_BOMB_COOLDOWN;
        smokeBombInvisibilityTimer = SMOKE_BOMB_INVISIBILITY_DURATION;
        
        // Drop smoke cloud
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 30; i++) {
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, 
                    this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ(), 
                    5, 1.0, 0.5, 1.0, 0.1);
            }
        }
        
        // Become invisible
        mob.setInvisibilityState(true, SMOKE_BOMB_INVISIBILITY_DURATION);
    }
    
    private void performSmokeBombDashStrike(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        // Dash strike
        Vec3d direction = target.getPos().subtract(this.mob.getPos()).normalize();
        Vec3d strikePos = target.getPos().subtract(direction.multiply(1.0));
        this.mob.setPosition(strikePos.x, this.mob.getY(), strikePos.z);
        
        // Attack
        if (this.mob.isInAttackRange(target)) {
            float damage = (float) this.mob.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
            target.damage(this.mob.getDamageSources().mobAttack(this.mob), damage);
        }
    }
    
    // Ability 4: Shadow Clone
    private void performShadowClone(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("shadow_clone");
        shadowCloneCooldown = SHADOW_CLONE_COOLDOWN;
        
        // Spawn 1-2 clones near the target
        int cloneCount = this.mob.getRandom().nextInt(2) + 1; // 1 or 2 clones
        
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < cloneCount; i++) {
                // Spawn clone near target
                double angle = (i * 2.0 * Math.PI / cloneCount) + this.mob.getRandom().nextDouble() * 0.5;
                double distance = 2.0 + this.mob.getRandom().nextDouble() * 2.0;
                double x = target.getX() + Math.cos(angle) * distance;
                double z = target.getZ() + Math.sin(angle) * distance;
                double y = target.getY();
                
                // Find safe spawn position
                net.minecraft.util.math.BlockPos spawnPos = new net.minecraft.util.math.BlockPos((int)x, (int)y, (int)z);
                while (!serverWorld.getBlockState(spawnPos).isAir() && spawnPos.getY() < serverWorld.getTopY()) {
                    spawnPos = spawnPos.up();
                }
                
                JoeRebelCloneEntity clone = ModEntities.JOE_REBEL_CLONE.create(serverWorld);
                if (clone != null) {
                    clone.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 
                        this.mob.getRandom().nextFloat() * 360.0f, 0.0f);
                    clone.setTarget(target);
                    clone.setLifetime(200); // 10 seconds
                    serverWorld.spawnEntity(clone);
                    
                    // Spawn smoke particles
                    for (int j = 0; j < 10; j++) {
                        serverWorld.spawnParticles(ParticleTypes.SMOKE, 
                            clone.getX(), clone.getY() + 0.5, clone.getZ(), 
                            5, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }
    }
    
    // Ability 5: Arrow Rain
    private void performArrowRain(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("arrow_rain");
        arrowRainCooldown = ARROW_RAIN_COOLDOWN;
        
        // Create large AOE of arrows coming down near target
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            double centerX = target.getX();
            double centerZ = target.getZ();
            double centerY = target.getY() + 20.0; // Start 20 blocks above
            
            double radius = 8.0; // 8 block radius AOE
            int arrowCount = 30; // 30 arrows
            
            // Visual warning - particles on ground
            for (int i = 0; i < 50; i++) {
                double angle = this.mob.getRandom().nextDouble() * 2.0 * Math.PI;
                double r = this.mob.getRandom().nextDouble() * radius;
                double x = centerX + Math.cos(angle) * r;
                double z = centerZ + Math.sin(angle) * r;
                serverWorld.spawnParticles(ParticleTypes.SMOKE, x, target.getY() + 0.1, z, 1, 0, 0, 0, 0.05);
            }
            
            // Delay arrow spawn by 1 second (20 ticks)
            serverWorld.getServer().execute(() -> {
                for (int i = 0; i < arrowCount; i++) {
                    double angle = this.mob.getRandom().nextDouble() * 2.0 * Math.PI;
                    double r = this.mob.getRandom().nextDouble() * radius;
                    double x = centerX + Math.cos(angle) * r;
                    double z = centerZ + Math.sin(angle) * r;
                    double y = centerY + this.mob.getRandom().nextDouble() * 5.0; // Vary height slightly
                    
                    ArrowEntity arrow = EntityType.ARROW.create(serverWorld);
                    if (arrow != null) {
                        arrow.refreshPositionAndAngles(x, y, z, 0.0f, 90.0f); // Point straight down
                        arrow.setOwner(this.mob);
                        arrow.setVelocity(0, -2.0, 0); // Fast downward velocity
                        arrow.setDamage(6.0); // Moderate damage
                        arrow.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
                        serverWorld.spawnEntity(arrow);
                    }
                }
            });
        }
    }
    
    // Ability 6 & 7: Phase abilities are handled in the entity tick
    
    // Nightfall Execution (Ultimate)
    private void performNightfallExecution(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("nightfall");
        mob.setNightfallExecutionUsed();
        nightfallChargeTimer = NIGHTFALL_CHARGE_DURATION;
        
        // Teleport to arena edge
        Vec3d direction = target.getPos().subtract(this.mob.getPos()).normalize();
        Vec3d edgePos = target.getPos().subtract(direction.multiply(15.0)); // 15 blocks away
        this.mob.setPosition(edgePos.x, this.mob.getY(), edgePos.z);
        
        // Face target
        this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
        
        // Visual charge effect
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 50; i++) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, 
                    this.mob.getX(), this.mob.getY() + 1, this.mob.getZ(), 
                    10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
    
    private void performNightfallDash(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        // Dash across entire arena
        Vec3d startPos = this.mob.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(startPos).normalize();
        
        // Move entity along path
        this.mob.setVelocity(direction.multiply(3.0)); // Fast dash
        
        // Create cutting arc visual trail
        if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 30; i++) {
                double t = i / 30.0;
                double x = startPos.x + (targetPos.x - startPos.x) * t;
                double z = startPos.z + (targetPos.z - startPos.z) * t;
                serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, this.mob.getY() + 1, z, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private void checkNightfallHit(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        // Check if target was hit during dash
        if (this.mob.squaredDistanceTo(target) <= 4.0) {
            // Big damage
            float damage = (float) (this.mob.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) * 3.0);
            target.damage(this.mob.getDamageSources().mobAttack(this.mob), damage);
        } else {
            // Missed - become tired
            mob.setTired(40); // 2 seconds
        }
    }
    
    // Regular melee attack
    private void performMeleeAttack(LivingEntity target) {
        if (this.mob.getWorld().isClient()) return;
        
        mob.setAttackState("melee");
        this.mob.swingHand(Hand.MAIN_HAND);
        this.mob.tryAttack(target);
    }

    protected void resetCooldown() {
        this.cooldown = this.getTickCount(20);
    }

    protected boolean isCooledDown() {
        return this.cooldown <= 0;
    }

    protected boolean canAttack(LivingEntity target) {
        String currentState = this.mob.getAttackState();
        if (!currentState.isEmpty() && !"dash".equals(currentState) && !"nightfall".equals(currentState)) {
            return false;
        }
        
        return this.isCooledDown() && 
               (this.mob.isInAttackRange(target) || this.mob.squaredDistanceTo(target) <= 256.0) && 
               this.mob.getVisibilityCache().canSee(target);
    }

    protected int getTickCount(int ticks) {
        return ticks;
    }
}

