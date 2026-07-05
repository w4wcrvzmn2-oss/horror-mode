package ru.exeswi.exest.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cheap multiplayer hunt coordination. Counts how many horror mobs currently hunt each
 * player (recomputed at most once a second) so new hunters pick the least-terrorized
 * player. This splits groups apart and lets one player be stalked while the others
 * see nothing.
 */
public final class PackCoordinator {

    private static final Map<UUID, Integer> huntersPerPlayer = new HashMap<>();
    private static long lastComputeTime = Long.MIN_VALUE;

    private PackCoordinator() {
    }

    @Nullable
    public static PlayerEntity pickTarget(AbstractHorrorEntity mob, double range) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        recomputeIfStale(world);

        PlayerEntity best = null;
        int bestCount = Integer.MAX_VALUE;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double dist = mob.squaredDistanceTo(player);
            if (dist > range * range) {
                continue;
            }
            int count = huntersPerPlayer.getOrDefault(player.getUuid(), 0);
            if (count < bestCount || (count == bestCount && dist < bestDist)) {
                best = player;
                bestCount = count;
                bestDist = dist;
            }
        }
        return best;
    }

    /** How many horror mobs are currently locked onto this player. */
    public static int huntersOf(PlayerEntity player) {
        recomputeIfStale((ServerWorld) player.getWorld());
        return huntersPerPlayer.getOrDefault(player.getUuid(), 0);
    }

    private static void recomputeIfStale(ServerWorld world) {
        long now = world.getTime();
        if (now - lastComputeTime < 20) {
            return;
        }
        lastComputeTime = now;
        huntersPerPlayer.clear();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof AbstractHorrorEntity horror) {
                LivingEntity target = horror.getTarget();
                if (target instanceof PlayerEntity player) {
                    huntersPerPlayer.merge(player.getUuid(), 1, Integer::sum);
                }
            }
        }
    }
}
