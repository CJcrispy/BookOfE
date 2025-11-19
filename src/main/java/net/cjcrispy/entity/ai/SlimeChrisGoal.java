package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.SlimeChrisEntity;
import net.cjcrispy.procedure.chris.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SlimeChrisGoal extends Goal {
    private final SlimeChrisEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int ticksUntilNextAttack;
    private static final int DEFAULT_ATTACK_INTERVAL = 20;
    private long lastUpdateTime;

    private final List<AttackPhase> attackPhases = new ArrayList<>();
    private int currentAttackIndex = 0;

    public SlimeChrisGoal(SlimeChrisEntity mob, double speed, boolean pauseWhenMobIdle) {
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
        LivingEntity livingEntity = mob.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) {
            return false;
        } else if (!pauseWhenMobIdle) {
            return !mob.getNavigation().isIdle();
        } else {
            return mob.isInWalkTargetRange(livingEntity.getBlockPos());
        }
    }

    @Override
    public void start() {
        mob.getNavigation().startMovingAlong(path, speed);
        mob.setAttacking(true);
        updateCountdownTicks = 0;
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = mob.getTarget();
        if (livingEntity != null) {
            mob.setTarget(null);
        }
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
        // Melee attack with slime hammer
        attackPhases.add(new AttackPhase(
                SlimeChrisGoal::isInMeleeRange,
                context -> {
                    SlimeMeleeAttack.execute(context.mob, context.target);
                },
                DEFAULT_ATTACK_INTERVAL
        ));

        // Slime Meteor - can be used at medium range
        attackPhases.add(new AttackPhase(
                context -> context.mob.squaredDistanceTo(context.target) <= 256 && context.mob.squaredDistanceTo(context.target) >= 16,
                context -> {
                    SlimeMeteor.execute(context.mob, context.target);
                },
                DEFAULT_ATTACK_INTERVAL + 40
        ));

        // Summon custom slime
        attackPhases.add(new AttackPhase(
                context -> true,
                context -> {
                    SummonCustomSlime.execute(context.mob);
                },
                DEFAULT_ATTACK_INTERVAL + 60
        ));
    }

    private static boolean isInMeleeRange(AttackContext context) {
        return context.mob.isInAttackRange(context.target) && context.mob.getVisibilityCache().canSee(context.target);
    }

    public void addAttackPhase(Predicate<AttackContext> canExecute,
                                Consumer<AttackContext> action, int cooldownTicks) {
        attackPhases.add(new AttackPhase(canExecute, action, cooldownTicks));
    }

    private record AttackContext(SlimeChrisEntity mob, LivingEntity target) { }

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

