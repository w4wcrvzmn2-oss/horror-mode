package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

import java.util.EnumSet;

/**
 * Slow, deliberate walk towards the player, mostly while they are not watching.
 * Rarely breaks into a short sprint. Stops at whisper distance and just stands there.
 */
public class CreepTowardsGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private final double slowSpeed;
    private final double sprintSpeed;
    private final float sprintChance;
    private PlayerEntity player;
    private int sprintTicks;

    public CreepTowardsGoal(AbstractHorrorEntity mob, double slowSpeed, double sprintSpeed, float sprintChance) {
        this.mob = mob;
        this.slowSpeed = slowSpeed;
        this.sprintSpeed = sprintSpeed;
        this.sprintChance = sprintChance;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null || mob.getRandom().nextInt(60) != 0) {
            return false;
        }
        player = mob.closestSurvivalPlayer(40.0);
        return player != null && mob.squaredDistanceTo(player) > 36.0;
    }

    @Override
    public boolean shouldContinue() {
        return mob.getTarget() == null && player != null && player.isAlive()
                && mob.squaredDistanceTo(player) > 25.0;
    }

    @Override
    public void stop() {
        player = null;
        sprintTicks = 0;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getLookControl().lookAt(player, 30.0f, 30.0f);
        if (sprintTicks > 0) {
            sprintTicks--;
        } else if (mob.getRandom().nextFloat() < sprintChance / 20.0f) {
            sprintTicks = 20 + mob.getRandom().nextInt(30);
        }
        boolean watched = HorrorUtil.isLookedAtBy(player, mob);
        if (watched && sprintTicks <= 0) {
            // freeze mid-step while observed, resume when the player looks away
            mob.getNavigation().stop();
            return;
        }
        mob.getNavigation().startMovingTo(player, sprintTicks > 0 ? sprintSpeed : slowSpeed);
    }
}
