package ru.exeswi.exest.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Geometry and world helpers shared by AI goals and horror events. */
public final class HorrorUtil {

    private HorrorUtil() {
    }

    /**
     * True when the given position lies inside the player's view cone.
     * Used to decide where things may appear or vanish without being noticed.
     */
    public static boolean isInViewCone(PlayerEntity player, Vec3d pos) {
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d toPos = pos.subtract(player.getEyePos());
        double len = toPos.length();
        if (len < 1.0e-3) {
            return true;
        }
        // ~70 degrees half-angle, generous enough to cover the whole screen
        return look.dotProduct(toPos.normalize()) > 0.342;
    }

    /** True when the player both faces the entity and has unobstructed line of sight to it. */
    public static boolean isLookedAtBy(PlayerEntity player, Entity target) {
        return isInViewCone(player, target.getBoundingBox().getCenter()) && hasLineOfSight(player, target);
    }

    public static boolean hasLineOfSight(PlayerEntity player, Entity target) {
        Vec3d from = player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter();
        HitResult hit = player.getWorld().raycast(new RaycastContext(
                from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS
                || hit.getPos().squaredDistanceTo(to) < 1.0;
    }

    /**
     * Finds a standable ground position on a ring around the center, preferring spots
     * outside the player's view. Returns null when nothing suitable was found.
     */
    @Nullable
    public static BlockPos findGroundSpot(ServerWorld world, PlayerEntity player, double minRadius,
                                          double maxRadius, boolean outsideView, Random random) {
        Vec3d center = player.getPos();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = MathHelper.floor(center.x + Math.cos(angle) * dist);
            int z = MathHelper.floor(center.z + Math.sin(angle) * dist);
            BlockPos pos = findStandablePos(world, x, MathHelper.floor(center.y), z);
            if (pos == null) {
                continue;
            }
            if (outsideView && isInViewCone(player, Vec3d.ofCenter(pos))) {
                continue;
            }
            return pos;
        }
        return null;
    }

    /**
     * Looks for two air blocks above solid ground near the given column, first scanning
     * around the reference height (so it works in caves), then falling back to the surface.
     */
    @Nullable
    public static BlockPos findStandablePos(ServerWorld world, int x, int refY, int z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(x, refY, z);
        for (int dy = 0; dy <= 8; dy++) {
            for (int sign : new int[]{1, -1}) {
                pos.setY(refY + dy * sign);
                if (isStandable(world, pos)) {
                    return pos.toImmutable();
                }
            }
        }
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        pos.setY(topY);
        return isStandable(world, pos) ? pos.toImmutable() : null;
    }

    public static boolean isStandable(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty();
    }

    /** A point directly behind the player at the given distance, snapped to walkable ground. */
    @Nullable
    public static BlockPos posBehindPlayer(ServerWorld world, PlayerEntity player, double distance) {
        Vec3d back = player.getPos().subtract(player.getRotationVec(1.0f).multiply(distance, 0, distance));
        return findStandablePos(world, MathHelper.floor(back.x), MathHelper.floor(player.getY()), MathHelper.floor(back.z));
    }

    public static boolean isNightOrDark(World world, BlockPos pos) {
        return world.isNight() || world.getLightLevel(pos) < 6;
    }

    /** "Home" heuristic: a wooden door within the given radius of the position. */
    public static boolean isNearDoor(World world, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, 4, radius)) {
            if (world.getBlockState(pos).isIn(net.minecraft.registry.tag.BlockTags.WOODEN_DOORS)) {
                return true;
            }
        }
        return false;
    }
}
