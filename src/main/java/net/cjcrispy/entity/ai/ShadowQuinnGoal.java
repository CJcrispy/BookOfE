package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.ShadowQuinnEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class ShadowQuinnGoal extends Goal {
    protected final ShadowQuinnEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private long lastUpdateTime;
    private int spinAttackHitTimer = 0; // Timer for multi-hits during spin attack
    private static final int SPIN_ATTACK_HIT_INTERVAL = 5; // Hit every 5 ticks during spin
    private static final int SPIN_ATTACK_HIT_START = (int) Math.ceil(1.16 * 20); // Hit starts at 1.16 seconds (23 ticks)
    private static final int SPIN_ATTACK_HIT_END = (int) Math.ceil(2.4 * 20); // Hit ends at 2.4 seconds (48 ticks)
    private int chargedAttackHitTimer = 0; // Timer for delayed hit in charged attack
    private static final int CHARGED_ATTACK_HIT_DELAY = (int) Math.ceil(1.96 * 20); // Hit at 1.96 seconds (39 ticks)
    private int regularAttackHitTimer = 0; // Timer for delayed hit in regular attack
    private static final int REGULAR_ATTACK_HIT_DELAY = (int) Math.ceil(1.0 * 20); // Hit at 1.0 seconds (20 ticks)

    public ShadowQuinnGoal(ShadowQuinnEntity mob, double speed, boolean pauseWhenMobIdle) {
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
        if (livingEntity != null) {
            this.mob.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
            this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);

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
            
            // Stop navigation during attack animations to prevent interruptions
            String currentAttackState = this.mob.getAttackState();
            if (!currentAttackState.isEmpty() && !"spinattack".equals(currentAttackState)) {
                // Stop movement during charged and regular attacks (spin allows movement)
                this.mob.getNavigation().stop();
            }
            
            // Handle regular attack delayed hit
            if ("attack".equals(currentAttackState) && this.regularAttackHitTimer > 0) {
                this.regularAttackHitTimer--;
                if (this.regularAttackHitTimer == 0 && this.mob.isInAttackRange(livingEntity)) {
                    // Perform the actual hit after delay (at impact point in animation)
                    this.mob.tryAttack(livingEntity);
                }
            }
            
            // Handle charged attack delayed hit
            if ("chargedattack".equals(currentAttackState) && this.chargedAttackHitTimer > 0) {
                this.chargedAttackHitTimer--;
                if (this.chargedAttackHitTimer == 0 && this.mob.isInAttackRange(livingEntity)) {
                    // Perform the actual hit after delay
                    this.performChargedAttackHit(livingEntity);
                }
            }
            
            // Handle spin attack multi-hits - continue hitting while spin animation is active
            if ("spinattack".equals(currentAttackState) && this.spinAttackHitTimer > 0) {
                // Calculate how many ticks have passed since animation started
                int ticksElapsed = 52 - this.spinAttackHitTimer; // Total timer is 52 ticks
                
                // Only hit during the active hit window (1.16 to 2.4 seconds = 23 to 48 ticks)
                if (ticksElapsed >= SPIN_ATTACK_HIT_START && ticksElapsed <= SPIN_ATTACK_HIT_END) {
                    // Hit every SPIN_ATTACK_HIT_INTERVAL ticks during the active window
                    if ((ticksElapsed - SPIN_ATTACK_HIT_START) % SPIN_ATTACK_HIT_INTERVAL == 0) {
                        // Only hit if target is in melee range
                        if (this.mob.isInAttackRange(livingEntity)) {
                            this.performSpinAttackHits(livingEntity);
                        }
                    }
                }
                this.spinAttackHitTimer--;
            }
            
            this.attack(livingEntity);
        }
    }

    // Attack method using a switch statement to dynamically choose the attack type
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();

            // Dynamically choose the current attack type
            String attackType = this.getCurrentAttack(target);
            
            switch (attackType) {
                case "Melee":
                    // Set animation state for regular attack
                    this.mob.setAttackState("attack");
                    this.mob.swingHand(Hand.MAIN_HAND);
                    // Start delayed hit timer - hit happens during the animation at impact point
                    this.regularAttackHitTimer = REGULAR_ATTACK_HIT_DELAY;
                    // Don't attack immediately - wait for delayed hit
                    break;

                case "Charged":
                    // Set animation state for charged attack
                    this.mob.setAttackState("chargedattack");
                    this.mob.swingHand(Hand.MAIN_HAND);
                    // Start delayed hit timer - hit happens during the animation
                    this.chargedAttackHitTimer = CHARGED_ATTACK_HIT_DELAY;
                    // Don't attack immediately - wait for delayed hit
                    break;

                case "Spin":
                    // Set animation state for spin attack
                    this.mob.setAttackState("spinattack");
                    // Initialize spin attack timer for multi-hits (52 ticks = 2.56 seconds)
                    this.spinAttackHitTimer = 52;
                    // Hits will occur during the 1.16-2.4 second window automatically
                    break;

                default:
                    this.mob.setAttackState("attack");
                    this.mob.swingHand(Hand.MAIN_HAND);
                    // Start delayed hit timer for default case too
                    this.regularAttackHitTimer = REGULAR_ATTACK_HIT_DELAY;
            }
        }
    }

    // Method to determine which attack type to perform (based on conditions)
    private String getCurrentAttack(LivingEntity target) {
        double distanceSq = this.mob.squaredDistanceTo(target);
        double random = this.mob.getRandom().nextDouble();

        // Close range - prefer spin attack
        if (distanceSq <= 16.0 && random < 0.3) {
            return "Spin";
        }
        
        // Medium range - prefer charged attack
        if (distanceSq <= 64.0 && random < 0.4) {
            return "Charged";
        }
        
        // Default to melee attack
        return "Melee";
    }

    // Resets the cooldown between attacks
    protected void resetCooldown() {
        this.cooldown = this.getTickCount(20); // Set cooldown to 20 ticks (1 second)
    }

    // Check if ShadowQuinn is cooled down and ready to attack
    protected boolean isCooledDown() {
        return this.cooldown <= 0;
    }

    // Check if ShadowQuinn can attack the target (distance, visibility, cooldown, etc.)
    protected boolean canAttack(LivingEntity target) {
        // Allow attacks if cooled down, in range, and can see target
        // But skip if already in a non-spin attack (spin attack allows movement/continuation)
        String currentState = this.mob.getAttackState();
        if (!currentState.isEmpty() && !"spinattack".equals(currentState)) {
            return false; // Don't start new attack while non-spin attack is playing
        }
        
        return this.isCooledDown() && 
               this.mob.isInAttackRange(target) && 
               this.mob.getVisibilityCache().canSee(target);
    }
    
    // Perform charged attack hit (delayed)
    private void performChargedAttackHit(LivingEntity target) {
        if (target == null || !target.isAlive() || !this.mob.isInAttackRange(target)) {
            return;
        }
        
        // Perform the charged attack - deals more damage
        this.mob.tryAttack(target);
        
        // Potential for multiple hits with charged attack (30% chance)
        if (this.mob.getRandom().nextFloat() < 0.3f && this.mob.isInAttackRange(target)) {
            this.mob.tryAttack(target);
        }
    }
    
    // Perform spin attack hits on all nearby entities in range
    private void performSpinAttackHits(LivingEntity mainTarget) {
        double spinRange = 3.0;
        double spinRangeSq = spinRange * spinRange;
        
        // Hit main target if in range
        if (this.mob.squaredDistanceTo(mainTarget) <= spinRangeSq && mainTarget.isAlive()) {
            this.mob.tryAttack(mainTarget);
        }
        
        // Hit all nearby entities
        for (LivingEntity nearbyEntity : this.mob.getWorld().getEntitiesByClass(
                LivingEntity.class,
                this.mob.getBoundingBox().expand(spinRange),
                entity -> entity != this.mob && entity.isAlive() && this.mob.squaredDistanceTo(entity) <= spinRangeSq
        )) {
            // Only hit if in melee range
            if (this.mob.isInAttackRange(nearbyEntity)) {
                this.mob.tryAttack(nearbyEntity);
            }
        }
    }

    // Convert ticks to the required time for cooldowns
    protected int getTickCount(int ticks) {
        return ticks;
    }
}
