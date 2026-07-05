package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.EnumSet;

/**
 * The "it left... right?" maneuver. The mob abandons its target, walks far away as if
 * losing interest, lurks in silence, then suddenly relocates right back near the player.
 */
public class RetreatFeintGoal extends Goal {

    private enum Phase { RETREAT, LURK, RETURN }

    private final AbstractHorrorEntity mob;
    private Phase phase;
    private LivingEntity victim;
    private int lurkTicks;
    private long nextUseTime;

    public RetreatFeintGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (mob.getWorld().getTime() < nextUseTime || mob.getTarget() == null) {
            return false;
        }
        // more likely for cautious individuals, thanks to the per-mob bias
        return mob.getRandom().nextFloat() < 0.002f + 0.006f * (1.0f - mob.unpredictability());
    }

    @Override
    public void start() {
        victim = mob.getTarget();
        mob.setTarget(null);
        phase = Phase.RETREAT;
        lurkTicks = 100 + mob.getRandom().nextInt(200);
        nextUseTime = mob.getWorld().getTime() + 1200;
    }

    @Override
    public boolean shouldContinue() {
        return victim != null && victim.isAlive() && phase != null;
    }

    @Override
    public void stop() {
        victim = null;
        phase = null;
    }

    @Override
    public void tick() {
        switch (phase) {
            case RETREAT -> {
                if (mob.squaredDistanceTo(victim) > 30.0 * 30.0 || mob.getNavigation().isIdle()) {
                    double dx = mob.getX() - victim.getX();
                    double dz = mob.getZ() - victim.getZ();
                    double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                    if (len > 30.0) {
                        phase = Phase.LURK;
                    } else {
                        mob.getNavigation().startMovingTo(
                                mob.getX() + dx / len * 16.0, mob.getY(), mob.getZ() + dz / len * 16.0, 1.1);
                    }
                }
            }
            case LURK -> {
                mob.getNavigation().stop();
                if (--lurkTicks <= 0) {
                    phase = Phase.RETURN;
                }
            }
            case RETURN -> {
                mob.teleportOutsideView(8.0, 16.0);
                mob.setTarget(victim);
                phase = null;
            }
        }
    }
}
