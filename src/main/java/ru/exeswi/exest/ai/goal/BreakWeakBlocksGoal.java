package ru.exeswi.exest.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Nothing between it and you is permanent. When the path to its prey is blocked, the
 * creature digs — through dirt in a second, through stone in a few, through obsidian
 * eventually. It tunnels in any direction, including straight down to a player buried
 * in a sealed hole. Bedrock and containers are the only things it leaves alone.
 *
 * The digging is loud on purpose: rhythmic hits from behind the wall are the warning.
 */
public class BreakWeakBlocksGoal extends Goal {

    private static final int MAX_BREAK_TICKS = 140;

    private final AbstractHorrorEntity mob;
    private BlockPos targetPos;
    private int progress;
    private int breakTicks;

    public BreakWeakBlocksGoal(AbstractHorrorEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity victim = victim();
        if (victim == null) {
            return false;
        }
        if (!mob.getNavigation().isIdle() && !mob.horizontalCollision) {
            return false;
        }
        targetPos = findBlockToward(victim);
        return targetPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return targetPos != null && isBreakable(mob.getWorld().getBlockState(targetPos)) && victim() != null;
    }

    @Override
    public void start() {
        progress = 0;
        breakTicks = breakTime(mob.getWorld().getBlockState(targetPos));
    }

    @Override
    public void stop() {
        targetPos = null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }
        progress++;
        BlockState state = mob.getWorld().getBlockState(targetPos);
        if (progress % 5 == 0) {
            mob.swingHand(Hand.MAIN_HAND);
            mob.getWorld().playSound(null, targetPos, state.getSoundGroup().getHitSound(),
                    SoundCategory.HOSTILE, 0.8f, 0.6f + mob.getRandom().nextFloat() * 0.3f);
        }
        if (progress >= breakTicks) {
            mob.getWorld().breakBlock(targetPos, false, mob);
            targetPos = null;
        }
    }

    @Nullable
    private LivingEntity victim() {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            return target;
        }
        return mob.closestSurvivalPlayer(16.0);
    }

    /**
     * The next block on the straight line to the victim — horizontal, upward or
     * straight down. This is what lets it tunnel to a buried player.
     */
    @Nullable
    private BlockPos findBlockToward(LivingEntity victim) {
        BlockPos feet = mob.getBlockPos();
        Vec3d delta = victim.getPos().subtract(mob.getPos());
        int sx = MathHelper.sign(delta.x);
        int sy = MathHelper.sign(delta.y);
        int sz = MathHelper.sign(delta.z);
        boolean horizontalFirst = Math.abs(delta.x) + Math.abs(delta.z) >= Math.abs(delta.y);

        List<BlockPos> candidates = new ArrayList<>(6);
        BlockPos horizEye = feet.add(Math.abs(delta.x) >= Math.abs(delta.z) ? sx : 0, 1,
                Math.abs(delta.z) > Math.abs(delta.x) ? sz : 0);
        BlockPos horizFeet = horizEye.down();
        if (horizontalFirst) {
            candidates.add(horizEye);
            candidates.add(horizFeet);
        }
        if (sy < 0) {
            candidates.add(feet.down());
        } else if (sy > 0) {
            candidates.add(feet.up(2));
        }
        if (!horizontalFirst) {
            candidates.add(horizEye);
            candidates.add(horizFeet);
        }
        for (BlockPos pos : candidates) {
            if (isBreakable(mob.getWorld().getBlockState(pos))) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private boolean isBreakable(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof DoorBlock || state.hasBlockEntity()) {
            return false; // doors have their own goal; chests are left untouched
        }
        // hardness below zero is bedrock-grade: the one true wall
        return state.getHardness(mob.getWorld(), targetPos == null ? mob.getBlockPos() : targetPos) >= 0.0f;
    }

    private int breakTime(BlockState state) {
        float hardness = Math.max(0.0f, state.getHardness(mob.getWorld(), targetPos));
        return Math.min(MAX_BREAK_TICKS, 20 + (int) (hardness * 12.0f));
    }
}
