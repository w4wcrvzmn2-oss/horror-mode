package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * When a player has been staring at the mob for too long it either vanishes in smoke or
 * relocates to a spot outside anyone's view. The threshold gets a per-mob random spread
 * so the behavior never becomes a reliable pattern.
 */
public class VanishWhenStaredGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private final int baseThresholdTicks;
    private final float teleportChance;

    public VanishWhenStaredGoal(AbstractHorrorEntity mob, int baseThresholdTicks, float teleportChance) {
        this.mob = mob;
        this.baseThresholdTicks = baseThresholdTicks;
        this.teleportChance = teleportChance;
    }

    @Override
    public boolean canStart() {
        if (mob.isEnraged()) {
            return false; // nothing saves you from the chase, staring included
        }
        int threshold = baseThresholdTicks + (int) (mob.unpredictability() * baseThresholdTicks);
        return mob.getStareTicks() > threshold;
    }

    @Override
    public void start() {
        if (mob.getRandom().nextFloat() < teleportChance && mob.teleportOutsideView(16.0, 32.0)) {
            mob.resetStare();
        } else {
            mob.vanish(true);
        }
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }
}
