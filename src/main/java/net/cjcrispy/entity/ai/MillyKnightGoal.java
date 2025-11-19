package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.procedure.common.CrossbowBarrageAbility;
import net.cjcrispy.procedure.milly.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MillyKnightGoal extends Goal {
    private final MillyKnightEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int ticksUntilNextAttack;
    private int restPeriodTicks = 0; // Tracks rest period between attacks
    private static final int DEFAULT_ATTACK_INTERVAL = 15; // shorter base cadence
    private static final int REST_PERIOD_DURATION = 30; // 1.5 seconds of rest between attacks (dance rhythm)
    private long lastUpdateTime;

    private final List<AttackPhase> attackPhases = new ArrayList<>();
    private int currentAttackIndex = 0;

    public MillyKnightGoal(MillyKnightEntity mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speed = speed;
        this.pauseWhenMobIdle = pauseWhenMobIdle;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        registerDefaultAttacks();
    }

    @Override
    public boolean canStart() {
        long currentTime = mob.getWorld().getTime();
        if (currentTime - lastUpdateTime < DEFAULT_ATTACK_INTERVAL) return false;

        lastUpdateTime = currentTime;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        path = mob.getNavigation().findPathTo(target, 0);
        return path != null || mob.isInAttackRange(target);
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && (!pauseWhenMobIdle || mob.isInWalkTargetRange(target.getBlockPos()));
    }

    @Override
    public void start() {
        mob.getNavigation().startMovingAlong(path, speed);
        mob.setAttacking(true);
        updateCountdownTicks = 0;
        ticksUntilNextAttack = 0;
        restPeriodTicks = 0;
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        mob.setAttacking(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        mob.getLookControl().lookAt(target, 30.0F, 30.0F);

        if (mob.isCrossbowBarrageActive()) {
            mob.getNavigation().stop();
            restPeriodTicks = Math.max(restPeriodTicks, 5);
            return;
        }
        
        // Handle rest period - during rest, slow down movement and don't attack
        if (restPeriodTicks > 0) {
            restPeriodTicks--;
            // Slow movement during rest period (dance rhythm - gives player time to heal)
            updateCountdownTicks = Math.max(updateCountdownTicks - 1, 0);
            if (updateCountdownTicks <= 0) {
                updateCountdownTicks = 4 + mob.getRandom().nextInt(7);
                // Use reduced speed during rest (30% of normal speed)
                double restSpeed = speed * 0.3;
                if (!mob.getNavigation().startMovingTo(target, restSpeed)) updateCountdownTicks += 15;
            }
            return; // Don't attack during rest period
        }

        // Normal movement when not resting
        updateCountdownTicks = Math.max(updateCountdownTicks - 1, 0);
        if (updateCountdownTicks <= 0) {
            updateCountdownTicks = 4 + mob.getRandom().nextInt(7);
            if (!mob.getNavigation().startMovingTo(target, speed)) updateCountdownTicks += 15;
        }

        // Only attack if not in rest period
        if (ticksUntilNextAttack > 0) {
            ticksUntilNextAttack--;
        } else {
            executeAttack(target);
        }
    }

    private void executeAttack(LivingEntity target) {
        AttackPhase phase = selectNextAttackPhase(target);
        if (phase == null) return;

        phase.perform(new AttackContext(mob, target));
        ticksUntilNextAttack = phase.cooldownTicks();
        
        // Start rest period after attack (creates dance rhythm - attack then pause)
        restPeriodTicks = REST_PERIOD_DURATION;
    }

    private AttackPhase selectNextAttackPhase(LivingEntity target) {
        if (attackPhases.isEmpty()) return null;

        AttackContext context = new AttackContext(mob, target);
        int checked = 0;

        while (checked < attackPhases.size()) {
            AttackPhase phase = attackPhases.get(currentAttackIndex);
            currentAttackIndex = (currentAttackIndex + 1) % attackPhases.size();

            if (phase.canExecute(context)) {
                return phase;
            }
            checked++;
        }

        return null;
    }

    private void registerDefaultAttacks() {
        attackPhases.add(new AttackPhase(
                MillyKnightGoal::isInMeleeRange,
                context -> {
                    context.mob.ensureBlackbornEquipped();
                    context.mob.swingHand(Hand.MAIN_HAND);
                    context.mob.tryAttack(context.target);
                },
                DEFAULT_ATTACK_INTERVAL
        ));

        attackPhases.add(new AttackPhase(
                context -> true,
                context -> {
                    CrossbowBarrageAbility.execute(context.mob, context.target);
                },
                DEFAULT_ATTACK_INTERVAL + 12
        ));

        attackPhases.add(new AttackPhase(
                context -> context.mob.squaredDistanceTo(context.target) <= 36,
                context -> {
                    context.mob.ensureBlackbornEquipped();
                    MillyKnightChainGrab.execute(context.mob);
                },
                DEFAULT_ATTACK_INTERVAL + 4
        ));

        attackPhases.add(new AttackPhase(
                context -> context.mob.squaredDistanceTo(context.target) <= 16,
                context -> {
                    context.mob.ensureBlackbornEquipped();
                    MillyKnightSpinngCrossSlash.execute(context.mob);
                },
                DEFAULT_ATTACK_INTERVAL + 8
        ));

        attackPhases.add(new AttackPhase(
                context -> context.mob.squaredDistanceTo(context.target) <= 49,
                context -> {
                    context.mob.ensureBlackbornEquipped();
                    MillyKnightChargingSlash.execute(context.mob);
                },
                DEFAULT_ATTACK_INTERVAL + 10
        ));
    }

    private static boolean isInMeleeRange(AttackContext context) {
        return context.mob.isInAttackRange(context.target) && context.mob.getVisibilityCache().canSee(context.target);
    }

    public void addAttackPhase(Predicate<AttackContext> canExecute, Consumer<AttackContext> action, int cooldownTicks) {
        attackPhases.add(new AttackPhase(canExecute, action, cooldownTicks));
    }

    private record AttackContext(MillyKnightEntity mob, LivingEntity target) { }

    private static final class AttackPhase {
        private final Predicate<AttackContext> canExecute;
        private final Consumer<AttackContext> action;
        private final int cooldownTicks;

        private AttackPhase(Predicate<AttackContext> canExecute, Consumer<AttackContext> action, int cooldownTicks) {
            this.canExecute = canExecute;
            this.action = action;
            this.cooldownTicks = Math.max(1, cooldownTicks);
        }

        private boolean canExecute(AttackContext context) {
            return canExecute.test(context);
        }

        private void perform(AttackContext context) {
            action.accept(context);
        }

        private int cooldownTicks() {
            return cooldownTicks;
        }
    }
}
