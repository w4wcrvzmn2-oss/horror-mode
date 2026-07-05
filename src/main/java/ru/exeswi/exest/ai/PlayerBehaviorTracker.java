package ru.exeswi.exest.ai;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.world.HorrorWorldState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Watches how players move and remembers the places where they hide: spots where a
 * player stays put for a while, especially enclosed or dark ones. Monsters later use
 * those spots for ambushes, so hiding in the same corner twice is a bad idea.
 */
public final class PlayerBehaviorTracker {

    private static final int STATIONARY_SECONDS_TO_LEARN = 30;

    private static final Map<UUID, Vec3d> anchor = new HashMap<>();
    private static final Map<UUID, Integer> stationarySeconds = new HashMap<>();

    private PlayerBehaviorTracker() {
    }

    /** Called once per second. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            Vec3d prev = anchor.get(id);
            if (prev == null || player.getPos().squaredDistanceTo(prev) > 4.0) {
                anchor.put(id, player.getPos());
                stationarySeconds.put(id, 0);
                continue;
            }
            int seconds = stationarySeconds.merge(id, 1, Integer::sum);
            if (seconds == STATIONARY_SECONDS_TO_LEARN && looksLikeHidingSpot(player)) {
                HorrorWorldState.get(player.getServerWorld())
                        .rememberHidingSpot(id, player.getBlockPos());
            }
        }
    }

    private static boolean looksLikeHidingSpot(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        boolean dark = world.getLightLevel(pos) < 7;
        boolean covered = !world.isSkyVisible(pos);
        return dark || covered || player.isSneaking();
    }

    /** A previously learned hiding spot near the player, or null when none is known. */
    @Nullable
    public static BlockPos getAmbushSpot(ServerPlayerEntity player, double maxDistance, Random random) {
        List<BlockPos> spots = HorrorWorldState.get(player.getServerWorld())
                .getHidingSpots(player.getUuid());
        List<BlockPos> near = spots.stream()
                .filter(p -> p.isWithinDistance(player.getBlockPos(), maxDistance))
                .toList();
        return near.isEmpty() ? null : near.get(random.nextInt(near.size()));
    }
}
