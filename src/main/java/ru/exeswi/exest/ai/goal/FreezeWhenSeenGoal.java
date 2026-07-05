package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

import java.util.EnumSet;

/**
 * Weeping-angel behavior: while any player has the mob on screen it does not move at
 * all. Registered with high priority so it overrides every movement goal.
 */
public class FreezeWhenSeenGoal extends Goal {

    private final AbstractHorrorEntity mob;

    public FreezeWhenSeenGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        return !mob.isEnraged() && isSeen();
    }

    @Override
    public boolean shouldContinue() {
        return !mob.isEnraged() && isSeen();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private boolean isSeen() {
        for (PlayerEntity player : mob.getWorld().getPlayers()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            if (player.squaredDistanceTo(mob) < 64.0 * 64.0 && HorrorUtil.isLookedAtBy(player, mob)) {
                return true;
            }
        }
        return false;
    }
}
