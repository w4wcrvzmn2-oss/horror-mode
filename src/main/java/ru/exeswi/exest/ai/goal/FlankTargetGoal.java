package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

import java.util.EnumSet;

/**
 * Instead of walking straight at its target the mob first circles to a point behind
 * the victim's back and approaches from there. Combined with pack targeting this makes
 * groups surround the player from several directions at once.
 */
public class FlankTargetGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private final double speed;
    private Vec3d flankPoint;
    private int recalcTimer;

    public FlankTargetGoal(AbstractHorrorEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = mob.getTarget();
        if (target == null || mob.isAttackAdvantageous()) {
            return false;
        }
        double distSq = mob.squaredDistanceTo(target);
        return distSq > 64.0 && distSq < 900.0 && mob.getRandom().nextInt(40) == 0;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = mob.getTarget();
        return target != null && flankPoint != null
                && mob.getPos().squaredDistanceTo(flankPoint) > 9.0;
    }

    @Override
    public void start() {
        recalcTimer = 0;
    }

    @Override
    public void stop() {
        flankPoint = null;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (--recalcTimer <= 0) {
            recalcTimer = 30;
            // a point 7 blocks behind the target relative to where they are looking
            Vec3d behind = target.getPos().subtract(target.getRotationVec(1.0f)
                    .multiply(7.0, 0.0, 7.0));
            BlockPos ground = HorrorUtil.findStandablePos(
                    (net.minecraft.server.world.ServerWorld) mob.getWorld(),
                    (int) behind.x, target.getBlockPos().getY(), (int) behind.z);
            flankPoint = ground != null ? Vec3d.ofBottomCenter(ground) : target.getPos();
            mob.getNavigation().startMovingTo(flankPoint.x, flankPoint.y, flankPoint.z, speed);
        }
    }
}
