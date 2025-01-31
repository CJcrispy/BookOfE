package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.procedure.common.DashAbility;
import net.cjcrispy.procedure.milly.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class MillyKnightGoal extends Goal {
    protected final MillyKnightEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int DEFAULT_ATTACK_INTERVAL = 40; // Default cooldown (2 seconds)
    private long lastUpdateTime;

    public MillyKnightGoal(MillyKnightEntity mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speed = speed;
        this.pauseWhenMobIdle = pauseWhenMobIdle;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        long currentTime = this.mob.getWorld().getTime();
        if (currentTime - this.lastUpdateTime < DEFAULT_ATTACK_INTERVAL) {
            return false;
        } else {
            this.lastUpdateTime = currentTime;
            LivingEntity target = this.mob.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().findPathTo(target, 0);
                return this.path != null || this.mob.isInAttackRange(target);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        } else if (!this.pauseWhenMobIdle) {
            return !this.mob.getNavigation().isIdle();
        } else {
            return this.mob.isInWalkTargetRange(target.getBlockPos());
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
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
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
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
            this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);

            if ((this.pauseWhenMobIdle || this.mob.getVisibilityCache().canSee(target)) && this.updateCountdownTicks <= 0) {
                this.targetX = target.getX();
                this.targetY = target.getY();
                this.targetZ = target.getZ();
                this.updateCountdownTicks = 4 + this.mob.getRandom().nextInt(7);
                double distance = this.mob.squaredDistanceTo(target);

                if (distance > 1024.0F) {
                    this.updateCountdownTicks += 10;
                } else if (distance > 256.0F) {
                    this.updateCountdownTicks += 5;
                }

                if (!this.mob.getNavigation().startMovingTo(target, this.speed)) {
                    this.updateCountdownTicks += 15;
                }

                this.updateCountdownTicks = this.getTickCount(this.updateCountdownTicks);
            }

            this.cooldown = Math.max(this.cooldown - 1, 0);
            this.attack(target);
        }
    }

    // Attack method with a switch case to select attacks dynamically
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();

            switch (this.getCurrentAttack()) {
                case "Melee":
                    System.out.println("MillyKnight performs a melee attack!");

                    // Equip an axe for melee combat (optional)
                    mob.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_AXE));

                    // Perform melee attack
                    mob.swingHand(Hand.MAIN_HAND);
                    mob.tryAttack(target);
                    break;

                case "Dash":
                    System.out.println("MillyKnight dashes towards/away from the target!");
                    DashAbility.execute(mob); // Call the dash procedure
                    break;

                case "ChainGrab":
                    System.out.println("MillyKnight grabs the target with a chain!");
                    MillyKnightChainGrab.execute(mob); // Call the chain grab procedure
                    break;

                case "SpinningCrossSlash":
                    System.out.println("MillyKnight performs a spinning cross slash!");
                    MillyKnightSpinngCrossSlash.execute(mob); // Call the spinning cross slash procedure
                    break;

                case "ChargingSlash":
                    System.out.println("MillyKnight charges for a powerful slash!");
                    MillyKnightChargingSlash.execute(mob); // Call the charging slash procedure
                    break;

                default:
                    System.out.println("MillyKnight hesitates...");
            }
        }
    }

    // Randomly select an attack type
    private String getCurrentAttack() {
        double random = mob.getRandom().nextDouble();

        if (random < 0.2) {
            return "Melee";
        } else if (random < 0.4) {
            return "ChargingSlash";
        } else if (random < 0.6) {
            return "ChainGrab";
        } else if (random < 0.8) {
            return "SpinningCrossSlash";
        } else {
            return "Dash";
        }
    }

    // Resets the cooldown between attacks
    protected void resetCooldown() {
        this.cooldown = this.getTickCount(DEFAULT_ATTACK_INTERVAL);
    }

    // Check if MillyKnight is cooled down and ready to attack
    protected boolean isCooledDown() {
        return this.cooldown <= 0;
    }

    // Check if MillyKnight can attack the target (distance, visibility, cooldown, etc.)
    protected boolean canAttack(LivingEntity target) {
        return this.isCooledDown() && this.mob.isInAttackRange(target) && this.mob.getVisibilityCache().canSee(target);
    }

    // Convert ticks to the required time for cooldowns
    protected int getTickCount(int ticks) {
        return ticks;
    }
}
