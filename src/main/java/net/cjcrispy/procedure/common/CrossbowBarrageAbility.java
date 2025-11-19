package net.cjcrispy.procedure.common;

import net.cjcrispy.entity.custom.MillyKnightEntity;
import net.minecraft.entity.LivingEntity;

public final class CrossbowBarrageAbility {
    private static final int SHOTS = 6;
    private static final int INTERVAL_TICKS = 6;
    private static final float PROJECTILE_VELOCITY = 2.6F;

    private CrossbowBarrageAbility() {
    }

    public static void execute(MillyKnightEntity mob, LivingEntity target) {
        if (mob == null || target == null) return;
        if (mob.getWorld().isClient()) return;
        if (mob.isCrossbowBarrageActive()) return;

        mob.startCrossbowBarrage(target, SHOTS, INTERVAL_TICKS, PROJECTILE_VELOCITY);
    }
}

