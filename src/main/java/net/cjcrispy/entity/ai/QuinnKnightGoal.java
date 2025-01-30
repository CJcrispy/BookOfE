package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.QuinnKnightEntity;
import net.cjcrispy.procedure.quinn.QuinnKnightFireWaveSlash;
import net.cjcrispy.procedure.quinn.QuinnKnightSoulExplosion;
import net.cjcrispy.procedure.quinn.QuinnKnightLeapingSlam;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class QuinnKnightGoal extends Goal {
    protected final QuinnKnightEntity mob;  // Specific to QuinnKnightEntity
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private long lastUpdateTime;

    public QuinnKnightGoal(QuinnKnightEntity mob, double speed, boolean pauseWhenMobIdle) {
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
                this.targetX = livingEntity.getX();
                this.targetY = livingEntity.getY();
                this.targetZ = livingEntity.getZ();
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
            this.attack(livingEntity);
        }
    }

    // Attack method using a switch statement to dynamically choose the attack type
    protected void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();

            // Dynamically choose the current attack type
            switch (this.getCurrentAttack()) {
                case "Melee":
                    System.out.println("QuinnKnight performs a melee attack!");

                    // Equip the QuinnKnight with an iron sword
                    mob.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

                    // Perform the melee attack
                    mob.swingHand(Hand.MAIN_HAND);
                    mob.tryAttack(target);

                    break;

                case "Firewave":
                    System.out.println("QuinnKnight launches a Firewave attack!");
                    QuinnKnightFireWaveSlash.execute(mob);  // Call the firewave procedure
                    break;

                case "SoulExplosion":
                    System.out.println("QuinnKnight triggers a Soul Explosion!");
                    QuinnKnightSoulExplosion.execute(mob);  // Call the soul explosion procedure
                    break;

                case "LeapingSlam":
                    System.out.println("QuinnKnight performs a Leaping Slam attack!");
                    QuinnKnightLeapingSlam.execute(mob);  // Call the leaping slam procedure
                    break;

                default:
                    System.out.println("QuinnKnight does nothing...");
            }
        }
    }

    // Method to determine which attack type to perform (based on conditions)
    private String getCurrentAttack() {
        // Here we can determine the attack type dynamically. For now, we use random selection.
        double random = mob.getRandom().nextDouble();

        // Randomly choose between "Melee", "Firewave", "SoulExplosion", and "LeapingSlam"
        if (random < 0.25) {
            return "Melee";
        } else if (random < 0.5) {
            return "Firewave";
        } else if (random < 0.75) {
            return "SoulExplosion";
        } else {
            return "LeapingSlam";
        }
    }

    // Resets the cooldown between attacks
    protected void resetCooldown() {
        this.cooldown = this.getTickCount(20);  // Set cooldown to 20 ticks (1 second)
    }

    // Check if QuinnKnight is cooled down and ready to attack
    protected boolean isCooledDown() {
        return this.cooldown <= 0;
    }

    // Check if QuinnKnight can attack the target (distance, visibility, cooldown, etc.)
    protected boolean canAttack(LivingEntity target) {
        return this.isCooledDown() && this.mob.isInAttackRange(target) && this.mob.getVisibilityCache().canSee(target);
    }

    // Convert ticks to the required time for cooldowns
    protected int getTickCount(int ticks) {
        return ticks;
    }
}
