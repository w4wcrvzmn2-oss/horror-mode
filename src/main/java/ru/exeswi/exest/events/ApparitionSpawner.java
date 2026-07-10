package ru.exeswi.exest.events;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.registry.ModEntities;
import ru.exeswi.exest.util.HorrorUtil;

import java.util.List;

/**
 * Places both harmless apparitions ("you saw something") and real hunters into the
 * world. Placement strategies cover the classic sightings: on a hill against the sky,
 * between the trees, behind glass, underwater, in cave darkness, right behind you,
 * or in front of you for exactly one frame.
 */
public final class ApparitionSpawner {

    public enum Placement { ONE_FRAME, BEHIND, HILL, WINDOW, FOREST, UNDERWATER, CAVE, DISTANCE, IN_VIEW }

    /** Which creatures may appear as apparitions from the very start. */
    private static final List<EntityType<? extends AbstractHorrorEntity>> APPARITION_POOL = List.of(
            ModEntities.STALKER, ModEntities.SHADOW, ModEntities.SMILER,
            ModEntities.FACELESS, ModEntities.MIMIC);

    /** Hunter unlock order; index = minimum difficulty level. */
    private static final List<EntityType<? extends AbstractHorrorEntity>> HUNTER_UNLOCKS = List.of(
            ModEntities.STALKER, ModEntities.EYELESS_ZOMBIE, ModEntities.CRAWLER,
            ModEntities.BROKEN_VILLAGER, ModEntities.DISTORTED_ENDERMAN, ModEntities.SHADOW,
            ModEntities.SMILER, ModEntities.FLESH, ModEntities.PREDATOR, ModEntities.FACELESS);

    private ApparitionSpawner() {
    }

    /** Spawns a visual-only apparition. Returns it, or null when no spot was found. */
    @Nullable
    public static AbstractHorrorEntity spawnApparition(ServerPlayerEntity player, Placement placement) {
        ServerWorld world = player.getServerWorld();
        Random random = world.random;
        BlockPos pos = findSpot(world, player, placement, random);
        if (pos == null) {
            return null;
        }
        EntityType<? extends AbstractHorrorEntity> type =
                APPARITION_POOL.get(random.nextInt(APPARITION_POOL.size()));
        AbstractHorrorEntity entity = type.create(world);
        if (entity == null) {
            return null;
        }
        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                random.nextFloat() * 360.0f, 0.0f);
        int life = placement == Placement.ONE_FRAME ? 2 : 200 + random.nextInt(400);
        int minDistance = placement == Placement.ONE_FRAME ? 0 : 6;
        entity.becomeApparition(life, minDistance);
        if (placement == Placement.BEHIND) {
            entity.markSpawnedBehind();
        }
        return world.spawnEntity(entity) ? entity : null;
    }

    /** Spawns a real, hostile hunter outside the player's view. Respects the per-player cap. */
    public static boolean spawnHunter(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        // the cap scales with the group: four friends together deserve four times the company
        int groupSize = Math.max(1, world.getPlayers(p -> !p.isSpectator() && p.isAlive()
                && p.squaredDistanceTo(player) < 64.0 * 64.0).size());
        if (countHuntersAround(player) >= ConfigManager.get().maxHorrorMobsPerPlayer * groupSize) {
            return false;
        }
        BlockPos pos = HorrorUtil.findGroundSpot(world, player, 20.0, 40.0, true, world.random);
        if (pos == null) {
            return false;
        }
        // two creatures unlocked from the start, one more per level — variety early
        int maxIndex = Math.min(HUNTER_UNLOCKS.size() - 1, 1 + DifficultyScaler.level(world));
        EntityType<? extends AbstractHorrorEntity> type =
                HUNTER_UNLOCKS.get(world.random.nextInt(maxIndex + 1));
        AbstractHorrorEntity entity = type.create(world);
        if (entity == null) {
            return false;
        }
        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                world.random.nextFloat() * 360.0f, 0.0f);
        entity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        entity.applyDifficultyBonuses(world);
        entity.setMaxLife(20 * 60 * (4 + world.random.nextInt(5)));
        // vanilla despawn would quietly delete it before the player ever notices;
        // our own unseen-timeout handles the cleanup instead
        entity.setPersistent();
        boolean spawned = world.spawnEntity(entity);
        if (spawned && world.random.nextFloat() < 0.35f) {
            // some of them don't bother with the slow burn
            entity.setTarget(player);
        }
        return spawned;
    }

    public static int countHuntersAround(ServerPlayerEntity player) {
        return player.getServerWorld().getEntitiesByClass(AbstractHorrorEntity.class,
                net.minecraft.util.math.Box.of(player.getPos(), 128, 64, 128),
                e -> !e.isApparition()).size();
    }

    @Nullable
    private static BlockPos findSpot(ServerWorld world, ServerPlayerEntity player,
                                     Placement placement, Random random) {
        return switch (placement) {
            case ONE_FRAME -> inFrontOfPlayer(world, player, random);
            case BEHIND -> HorrorUtil.posBehindPlayer(world, player, 4.0 + random.nextDouble() * 3.0);
            case HILL -> onHill(world, player, random);
            case WINDOW -> behindWindow(world, player);
            case FOREST -> inForest(world, player, random);
            case UNDERWATER -> underwater(world, player, random);
            case CAVE -> HorrorUtil.findGroundSpot(world, player, 10.0, 25.0, true, random);
            case DISTANCE -> HorrorUtil.findGroundSpot(world, player, 30.0, 50.0, true, random);
            case IN_VIEW -> inView(world, player, 14.0, 35.0, random);
        };
    }

    /** A spot the player can see right now: inside the view cone, on walkable ground. */
    @Nullable
    private static BlockPos inView(ServerWorld world, ServerPlayerEntity player,
                                   double min, double max, Random random) {
        double yawRad = Math.toRadians(player.getYaw());
        for (int i = 0; i < 20; i++) {
            double offset = (random.nextDouble() - 0.5) * 1.1; // ±~30 degrees off center
            double dist = min + random.nextDouble() * (max - min);
            double x = player.getX() - Math.sin(yawRad + offset) * dist;
            double z = player.getZ() + Math.cos(yawRad + offset) * dist;
            BlockPos ground = HorrorUtil.findStandablePos(world,
                    MathHelper.floor(x), player.getBlockPos().getY(), MathHelper.floor(z));
            if (ground != null && HorrorUtil.isInViewCone(player, Vec3d.ofCenter(ground))) {
                return ground;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos inFrontOfPlayer(ServerWorld world, ServerPlayerEntity player, Random random) {
        Vec3d ahead = player.getPos().add(player.getRotationVec(1.0f)
                .multiply(6.0 + random.nextDouble() * 5.0, 0.0, 6.0 + random.nextDouble() * 5.0));
        return HorrorUtil.findStandablePos(world, MathHelper.floor(ahead.x),
                player.getBlockPos().getY(), MathHelper.floor(ahead.z));
    }

    @Nullable
    private static BlockPos onHill(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos best = null;
        for (int i = 0; i < 12; i++) {
            BlockPos candidate = HorrorUtil.findGroundSpot(world, player, 30.0, 60.0, false, random);
            if (candidate != null && world.isSkyVisible(candidate)
                    && (best == null || candidate.getY() > best.getY())) {
                best = candidate;
            }
        }
        return best;
    }

    @Nullable
    private static BlockPos behindWindow(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, 10, 4, 10)) {
            BlockState state = world.getBlockState(pos);
            if (!state.isIn(BlockTags.IMPERMEABLE)) {
                continue; // glass blocks only
            }
            // stand on the far side of the glass, away from the player
            net.minecraft.util.math.Direction away = net.minecraft.util.math.Direction.getFacing(
                    pos.getX() - playerPos.getX(), 0, pos.getZ() - playerPos.getZ());
            BlockPos outside = pos.offset(away);
            BlockPos ground = HorrorUtil.findStandablePos(world, outside.getX(), outside.getY(), outside.getZ());
            if (ground != null) {
                return ground;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos inForest(ServerWorld world, ServerPlayerEntity player, Random random) {
        for (int i = 0; i < 16; i++) {
            BlockPos candidate = HorrorUtil.findGroundSpot(world, player, 18.0, 40.0, true, random);
            if (candidate == null) {
                continue;
            }
            // "forest" here means: there is a leaf canopy close above the spot
            for (int dy = 2; dy <= 8; dy++) {
                if (world.getBlockState(candidate.up(dy)).isIn(BlockTags.LEAVES)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos underwater(ServerWorld world, ServerPlayerEntity player, Random random) {
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 8.0 + random.nextDouble() * 14.0;
            BlockPos surface = BlockPos.ofFloored(player.getPos().add(
                    Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            BlockPos.Mutable pos = surface.mutableCopy();
            // walk down through the water column to its floor
            int guard = 0;
            while (world.getFluidState(pos).getFluid() != Fluids.WATER && pos.getY() > world.getBottomY() && guard++ < 24) {
                pos.move(0, -1, 0);
            }
            if (world.getFluidState(pos).getFluid() != Fluids.WATER) {
                continue;
            }
            while (world.getFluidState(pos.down()).getFluid() == Fluids.WATER && pos.getY() > world.getBottomY()) {
                pos.move(0, -1, 0);
            }
            if (world.getFluidState(pos.up()).getFluid() == Fluids.WATER) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    /** True when the player is deep enough underground for cave sightings. */
    public static boolean isUnderground(ServerPlayerEntity player) {
        return !player.getServerWorld().isSkyVisible(player.getBlockPos())
                && player.getBlockPos().getY() < 50;
    }
}
