package ru.exeswi.exest.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * Opens closed wooden doors in the mob's path after a short, unsettling pause.
 * Doors are deliberately left open behind it.
 */
public class OpenDoorsGoal extends Goal {

    private final AbstractHorrorEntity mob;
    private BlockPos doorPos;
    private int delay;

    public OpenDoorsGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if (mob.getNavigation().isIdle()) {
            return false;
        }
        Direction dir = mob.getHorizontalFacing();
        for (BlockPos pos : new BlockPos[]{mob.getBlockPos(), mob.getBlockPos().offset(dir)}) {
            BlockState state = mob.getWorld().getBlockState(pos);
            if (state.isIn(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock
                    && !state.get(DoorBlock.OPEN)) {
                doorPos = pos;
                return true;
            }
        }
        return false;
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
            doorPos = null;
        }
    }
}
