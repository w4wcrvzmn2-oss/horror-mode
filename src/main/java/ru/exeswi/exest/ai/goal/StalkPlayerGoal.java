package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.EnumSet;

/**
 * Keeps the mob on a ring around the player, silently facing them. The mob drifts to
 * stay inside [minDistance, maxDistance] and otherwise just stands and watches.
 */
public class StalkPlayerGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private final double minDistance;
    private final double maxDistance;
    private final double speed;
    private PlayerEntity watched;

    public StalkPlayerGoal(AbstractHorrorEntity mob, double minDistance, double maxDistance, double speed) {
        this.mob = mob;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) {
            return false;
        }
        watched = mob.closestSurvivalPlayer(48.0);
        return watched != null;
    }

    @Override
    public boolean shouldContinue() {
        return mob.getTarget() == null && watched != null && watched.isAlive()
                && mob.squaredDistanceTo(watched) < 64.0 * 64.0;
    }

    @Override
    public void stop() {
        watched = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getLookControl().lookAt(watched, 30.0f, 30.0f);
        // rarely it gives its position away — a faint stare hum from the dark,
        // just enough for the player to turn around and find the silhouette
        if (mob.getRandom().nextInt(350) == 0) {
            mob.getWorld().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_STARE,
                    net.minecraft.sound.SoundCategory.HOSTILE, 0.6f, 0.45f);
        }
        double distSq = mob.squaredDistanceTo(watched);
        if (distSq > maxDistance * maxDistance) {
            mob.getNavigation().startMovingTo(watched, speed);
        } else if (distSq < minDistance * minDistance) {
            // back off, we don't want to be reachable
            double dx = mob.getX() - watched.getX();
            double dz = mob.getZ() - watched.getZ();
            double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            mob.getNavigation().startMovingTo(
                    mob.getX() + dx / len * 8.0, mob.getY(), mob.getZ() + dz / len * 8.0, speed);
        } else {
            mob.getNavigation().stop();
        }
    }
}
