package ru.exeswi.exest.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * Opens closed wooden doors near the mob after a short, unsettling pause. Scans a
 * small radius instead of just the facing direction, so diagonal approaches and
 * doors brushed while hunting are handled too. Doors are deliberately left open.
 */
public class OpenDoorsGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private BlockPos doorPos;
    private int delay;
    private long nextUseTime;

    public OpenDoorsGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if (mob.getWorld().getTime() < nextUseTime) {
            return false;
        }
        // only bother with doors while going somewhere or hunting someone
        if (mob.getNavigation().isIdle() && mob.getTarget() == null) {
            return false;
        }
        doorPos = findClosedDoor();
        return doorPos != null;
    }

    @Override
    public void start() {
        delay = 5 + mob.getRandom().nextInt(15);
    }

    @Override
    public boolean shouldContinue() {
        return delay > 0 && doorPos != null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (--delay <= 0 && doorPos != null) {
            BlockState state = mob.getWorld().getBlockState(doorPos);
            if (state.getBlock() instanceof DoorBlock door && !state.get(DoorBlock.OPEN)) {
                door.setOpen(mob, mob.getWorld(), state, doorPos, true);
            }
            nextUseTime = mob.getWorld().getTime() + 30;
            doorPos = null;
        }
    }

    private BlockPos findClosedDoor() {
        for (BlockPos pos : BlockPos.iterateOutwards(mob.getBlockPos(), 2, 1, 2)) {
            BlockState state = mob.getWorld().getBlockState(pos);
            if (state.isIn(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock
                    && !state.get(DoorBlock.OPEN)) {
                return pos.toImmutable();
            }
        }
        return null;
    }
}
