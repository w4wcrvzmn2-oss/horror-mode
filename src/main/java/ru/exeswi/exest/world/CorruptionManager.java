package ru.exeswi.exest.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.difficulty.DifficultyScaler;

import java.util.List;

/**
 * Slow environmental decay around players. Grass coarsens, plants wither, leaves rot
 * away, flesh-like growths surface, and — always off-screen — villagers and animals
 * quietly stop existing. Intensity follows the horror difficulty level.
 *
 * Performance: a fixed, tiny budget of block checks per player, twice a second.
 */
public final class CorruptionManager {

    private static final int BLOCK_SAMPLES_PER_PASS = 8;
    private static final double RADIUS = 40.0;

    private CorruptionManager() {
    }

    public static void tick(MinecraftServer server) {
        if (!ConfigManager.get().enableCorruption || server.getTicks() % 10 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isSpectator()) {
                corruptAround(player);
            }
        }
    }

    private static void corruptAround(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        float intensity = DifficultyScaler.level(world) / (float) DifficultyScaler.MAX_LEVEL;
        if (intensity <= 0.0f) {
            return;
        }
        Random random = world.random;
        for (int i = 0; i < BLOCK_SAMPLES_PER_PASS; i++) {
            if (random.nextFloat() > intensity * 0.6f) {
                continue;
            }
            BlockPos pos = randomColumnPos(player, random);
            transformBlock(world, pos, random);
        }
        despawnLife(world, player, intensity, random);
    }

    private static BlockPos randomColumnPos(ServerPlayerEntity player, Random random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 8.0 + random.nextDouble() * (RADIUS - 8.0);
        int x = (int) (player.getX() + Math.cos(angle) * dist);
        int z = (int) (player.getZ() + Math.sin(angle) * dist);
        int y = player.getServerWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
        return new BlockPos(x, y, z);
    }

    private static void transformBlock(ServerWorld world, BlockPos pos, Random random) {
        BlockState state = world.getBlockState(pos);

        if (state.isIn(BlockTags.LEAVES)) {
            // trees shed their leaves until only dead trunks remain
            world.removeBlock(pos, false);
            return;
        }
        if (state.isOf(Blocks.GRASS_BLOCK)) {
            world.setBlockState(pos, random.nextInt(20) == 0
                    ? Blocks.NETHER_WART_BLOCK.getDefaultState() // a patch of something like flesh
                    : Blocks.COARSE_DIRT.getDefaultState());
            return;
        }
        BlockState above = world.getBlockState(pos.up());
        if ((above.isOf(Blocks.SHORT_GRASS) || above.isOf(Blocks.TALL_GRASS)
                || above.isOf(Blocks.FERN) || above.isIn(BlockTags.SMALL_FLOWERS))
                && Blocks.DEAD_BUSH.getDefaultState().canPlaceAt(world, pos.up())) {
            world.setBlockState(pos.up(), Blocks.DEAD_BUSH.getDefaultState());
        }
    }

    /**
     * Villages empty out, pastures fall silent. Creatures are only ever removed when no
     * player can possibly witness it, so the world just feels progressively deserted.
     */
    private static void despawnLife(ServerWorld world, ServerPlayerEntity player,
                                    float intensity, Random random) {
        if (random.nextFloat() > intensity * 0.08f) {
            return;
        }
        Box area = Box.of(player.getPos(), 128, 64, 128);
        List<? extends net.minecraft.entity.LivingEntity> candidates = random.nextBoolean()
                ? world.getEntitiesByClass(VillagerEntity.class, area, e -> isUnwatched(world, e))
                : world.getEntitiesByClass(AnimalEntity.class, area, e -> isUnwatched(world, e));
        if (!candidates.isEmpty()) {
            candidates.get(random.nextInt(candidates.size())).discard();
        }
    }

    private static boolean isUnwatched(ServerWorld world, net.minecraft.entity.LivingEntity entity) {
        return world.getPlayers().stream()
                .allMatch(p -> p.squaredDistanceTo(entity) > 32.0 * 32.0);
    }
}
