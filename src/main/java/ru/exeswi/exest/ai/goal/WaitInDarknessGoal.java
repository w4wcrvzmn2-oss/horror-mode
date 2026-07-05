package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.EnumSet;

/**
 * When there is nothing better to do the mob seeks out a dark spot near the player,
 * stands in it and just... waits, facing them.
 */
public class WaitInDarknessGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private PlayerEntity player;
    private BlockPos darkSpot;
    private int waitTicks;

    public WaitInDarknessGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null || mob.getRandom().nextInt(100) != 0) {
            return false;
        }
        player = mob.closestSurvivalPlayer(40.0);
        if (player == null) {
            return false;
        }
        darkSpot = findDarkSpot();
        return darkSpot != null;
    }

    @Override
    public boolean shouldContinue() {
        return mob.getTarget() == null && player != null && player.isAlive()
                && darkSpot != null && waitTicks > 0;
    }

    @Override
    public void start() {
        waitTicks = 200 + mob.getRandom().nextInt(400);
        mob.getNavigation().startMovingTo(darkSpot.getX() + 0.5, darkSpot.getY(), darkSpot.getZ() + 0.5, 1.0);
    }

    @Override
    public void stop() {
        player = null;
        darkSpot = null;
    }

    @Override
    public void tick() {
        waitTicks--;
        if (mob.getBlockPos().isWithinDistance(darkSpot, 2.0)) {
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(player, 30.0f, 30.0f);
        } else if (mob.getNavigation().isIdle()) {
            mob.getNavigation().startMovingTo(darkSpot.getX() + 0.5, darkSpot.getY(), darkSpot.getZ() + 0.5, 1.0);
        }
    }

    @Nullable
    private BlockPos findDarkSpot() {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            double dist = 8.0 + mob.getRandom().nextDouble() * 12.0;
            BlockPos pos = BlockPos.ofFloored(player.getPos().add(
                    Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            BlockPos ground = ru.exeswi.exest.util.HorrorUtil.findStandablePos(
                    (net.minecraft.server.world.ServerWorld) mob.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (ground != null && mob.getWorld().getLightLevel(ground) < 5
                    && !ru.exeswi.exest.util.HorrorUtil.isInViewCone(player, Vec3d.ofCenter(ground))) {
                return ground;
            }
        }
        return null;
    }
}
