package ru.exeswi.exest.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A quiet observer's notes: every 30 seconds it writes down, in plain words, what
 * each player is doing. The journal event later quotes these notes back to the
 * player — including the one from exactly five minutes ago.
 */
public final class ActivityLog {

    private record Entry(long time, String text) {
    }

    private static final int SNAPSHOT_INTERVAL = 600;
    private static final int MAX_ENTRIES = 24;

    private static final Map<UUID, Deque<Entry>> LOG = new HashMap<>();
    private static final Map<UUID, Integer> BLOCKS_BROKEN = new HashMap<>();

    private ActivityLog() {
    }

    public static void onBlockBroken(ServerPlayerEntity player) {
        BLOCKS_BROKEN.merge(player.getUuid(), 1, Integer::sum);
    }

    /** Called from the main loop; samples every 30 seconds. */
    public static void tick(MinecraftServer server) {
        if (server.getTicks() % SNAPSHOT_INTERVAL != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }
            Deque<Entry> entries = LOG.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
            entries.addLast(new Entry(player.getServerWorld().getTime(), describe(player)));
            while (entries.size() > MAX_ENTRIES) {
                entries.removeFirst();
            }
        }
    }

    private static String describe(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int broken = BLOCKS_BROKEN.getOrDefault(player.getUuid(), 0);
        BLOCKS_BROKEN.put(player.getUuid(), 0);
        boolean underground = !world.isSkyVisible(player.getBlockPos());
        int light = world.getLightLevel(player.getBlockPos());

        if (player.isSleeping()) {
            return "спал. Так беззащитно";
        }
        if (broken > 10 && underground) {
            return String.format("копал в темноте на глубине Y=%d", player.getBlockPos().getY());
        }
        if (broken > 10) {
            return "ломал блоки и думал, что этот звук слышишь только ты";
        }
        if (player.isTouchingWater()) {
            return "плыл. В воде так плохо слышно, что сзади";
        }
        if (light < 5 && player.getVelocity().horizontalLengthSquared() < 0.001) {
            return "прятался в темноте и думал, что один";
        }
        if (underground) {
            return "бродил под землёй";
        }
        if (world.isNight()) {
            return "шёл сквозь ночь, слишком быстро, чтобы это было спокойствием";
        }
        return "бродил по поверхности при свете";
    }

    /** The note closest to the given number of ticks ago, e.g. 6000 = five minutes. */
    public static String activityAgo(ServerPlayerEntity player, long ticksAgo) {
        Deque<Entry> entries = LOG.get(player.getUuid());
        if (entries == null || entries.isEmpty()) {
            return "только пришёл в этот мир";
        }
        long targetTime = player.getServerWorld().getTime() - ticksAgo;
        Entry best = entries.peekLast();
        long bestDelta = Long.MAX_VALUE;
        for (Entry entry : entries) {
            long delta = Math.abs(entry.time - targetTime);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = entry;
            }
        }
        return best.text;
    }
}
