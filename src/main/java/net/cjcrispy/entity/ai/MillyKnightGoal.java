package net.cjcrispy.entity.ai;

import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.cjcrispy.item.ModItems;
import net.cjcrispy.procedure.common.DashAbility;
import net.cjcrispy.procedure.milly.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class MillyKnightGoal extends Goal {
    private final MillyKnightEntity mob;
    private final double speed;
    private final boolean pauseWhenMobIdle;
    private Path path;
    private int updateCountdownTicks;
    private int cooldown;
    private static final int ATTACK_INTERVAL = 40; // 2-second cooldown
    private long lastUpdateTime;

    // Enum to manage attack states
    private enum AttackState {
        MELEE, CHARGING_SLASH, CHAIN_GRAB, SPINNING_CROSS_SLASH, DASH
    }

    private AttackState currentState = AttackState.MELEE; // Default attack state

    public MillyKnightGoal(MillyKnightEntity mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speed = speed;
        this.pauseWhenMobIdle = pauseWhenMobIdle;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        long currentTime = mob.getWorld().getTime();
        if (currentTime - lastUpdateTime < ATTACK_INTERVAL) return false;

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
        cooldown = 0;
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
        updateCountdownTicks = Math.max(updateCountdownTicks - 1, 0);

        if (updateCountdownTicks <= 0) {
            updateCountdownTicks = 4 + mob.getRandom().nextInt(7);
            if (!mob.getNavigation().startMovingTo(target, speed)) updateCountdownTicks += 15;
        }

        cooldown = Math.max(cooldown - 1, 0);
        executeAttack(target);
    }

    // Executes the current attack based on the state
    private void executeAttack(LivingEntity target) {
        if (!canAttack(target)) return;

        resetCooldown();

        switch (currentState) {
            case MELEE:
                ItemStack weapon = new ItemStack(ModItems.BLACKBORN);
                mob.equipStack(EquipmentSlot.MAINHAND, weapon); // Equip in the main hand
                mob.setStackInHand(Hand.MAIN_HAND, weapon); // Ensure it updates visually
                mob.swingHand(Hand.MAIN_HAND);
                mob.tryAttack(target);
                break;


            case DASH:
                DashAbility.execute(mob);
                break;

            case CHAIN_GRAB:
                MillyKnightChainGrab.execute(mob);
                break;

            case SPINNING_CROSS_SLASH:
                MillyKnightSpinngCrossSlash.execute(mob);
                break;

            case CHARGING_SLASH:
                MillyKnightChargingSlash.execute(mob);
                break;
        }

        switchState(); // Change to the next attack after execution
    }

    // Switches to the next attack state in sequence
    private void switchState() {
        AttackState[] states = AttackState.values();
        int nextIndex = (currentState.ordinal() + 1) % states.length;
        currentState = states[nextIndex];
    }

    private void resetCooldown() {
        cooldown = ATTACK_INTERVAL;
    }

    private boolean canAttack(LivingEntity target) {
        return cooldown <= 0 && mob.isInAttackRange(target) && mob.getVisibilityCache().canSee(target);
    }
}
