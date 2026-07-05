package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import ru.exeswi.exest.ai.PlayerBehaviorTracker;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

/**
 * Occasional repositioning outside the player's vision: behind their back, into a
 * hiding spot the monsters have learned, or onto a random unseen point of the ring.
 */
public class AmbushTeleportGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private final int cooldownTicks;
    private long nextUseTime;

    public AmbushTeleportGoal(AbstractHorrorEntity mob, int cooldownTicks) {
        this.mob = mob;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public boolean canStart() {
        if (mob.getWorld().getTime() < nextUseTime) {
            return false;
        }
        return mob.closestSurvivalPlayer(40.0) != null && mob.getRandom().nextInt(80) == 0;
    }

    @Override
    public void start() {
        nextUseTime = mob.getWorld().getTime() + cooldownTicks + mob.getRandom().nextInt(cooldownTicks);
        ServerPlayerEntity player = mob.closestSurvivalPlayer(40.0);
        if (player == null) {
            return;
        }
        float roll = mob.getRandom().nextFloat();
        if (roll < 0.25f && ConfigManager.get().enableJumpscares) {
            BlockPos behind = HorrorUtil.posBehindPlayer((net.minecraft.server.world.ServerWorld) mob.getWorld(), player, 3.0);
            if (behind != null && mob.teleportToSpot(behind)) {
                return;
            }
        }
        if (roll < 0.6f) {
            BlockPos spot = PlayerBehaviorTracker.getAmbushSpot(player, 24.0, mob.getRandom());
            if (spot != null && mob.teleportToSpot(spot)) {
                return;
            }
        }
        mob.teleportOutsideView(12.0, 28.0);
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }
}
