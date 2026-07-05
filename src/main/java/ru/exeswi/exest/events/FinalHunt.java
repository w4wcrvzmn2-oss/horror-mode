package ru.exeswi.exest.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.entity.StalkerEntity;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.registry.ModEntities;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;
import ru.exeswi.exest.world.HorrorWorldState;

import java.util.UUID;

/**
 * The Final Hunt: at horror level 10 it stops hiding. The sky goes dark, waves of
 * enraged hunters descend, and then IT arrives — a named, unkillable-by-running boss
 * that does not vanish, does not fear light and does not stop. Kill it and the world
 * is cleansed: the horror timeline resets, sanity is restored, the sun comes back.
 * Survive without killing it and it simply... stays. Until next time.
 */
public final class FinalHunt {

    private static final int HUNT_DURATION_TICKS = 8000;
    private static final int RETRY_DELAY_TICKS = 72000;

    private static boolean active;
    private static String worldKey;
    private static long startedAtTick;
    private static UUID bossId;
    private static boolean bossSpawned;

    private FinalHunt() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Called every 100 ticks from the main loop. */
    public static void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (active) {
            run(server, world);
            return;
        }
        if (world.getPlayers().isEmpty()) {
            return;
        }
        HorrorWorldState state = HorrorWorldState.get(world);
        if (DifficultyScaler.level(world) >= DifficultyScaler.MAX_LEVEL
                && world.getTime() >= state.nextFinalHuntAt) {
            begin(world);
        }
    }

    public static void begin(ServerWorld world) {
        if (active || world.getPlayers().isEmpty()) {
            return;
        }
        active = true;
        worldKey = world.getRegistryKey().getValue().toString();
        startedAtTick = world.getServer().getTicks();
        bossId = null;
        bossSpawned = false;

        broadcast(world, "ОНО БОЛЬШЕ НЕ ПРЯЧЕТСЯ.", Formatting.DARK_RED, true);
        world.setWeather(0, HUNT_DURATION_TICKS, true, true);
        HorrorEventManager manager = Exest.eventManager();
        for (ServerPlayerEntity player : world.getPlayers()) {
            manager.mood(player, 0.6f, 0.4f, 0, HUNT_DURATION_TICKS);
            HorrorNetworking.sendCueBehind(player, SoundCue.STING, 1.2f);
            HorrorNetworking.sendCueBehind(player, SoundCue.VOICES, 1.0f);
            HorrorNetworking.sendCueBehind(player, SoundCue.HEARTBEAT, 1.0f);
        }
        MinecraftServer server = world.getServer();
        manager.schedule(server, 200, () -> spawnWave(world));
        manager.schedule(server, 800, () -> spawnWave(world));
        manager.schedule(server, 1400, () -> spawnBoss(world));
    }

    private static void spawnWave(ServerWorld world) {
        if (!active) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers(p -> !p.isSpectator() && p.isAlive())) {
            for (int i = 0; i < 2; i++) {
                BlockPos pos = HorrorUtil.findGroundSpot(world, player, 15.0, 30.0, true, world.random);
                if (pos == null) {
                    continue;
                }
                AbstractHorrorEntity hunter = (world.random.nextBoolean()
                        ? ModEntities.STALKER : ModEntities.SMILER).create(world);
                if (hunter == null) {
                    continue;
                }
                hunter.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
                hunter.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
                hunter.setPersistent();
                if (world.spawnEntity(hunter)) {
                    hunter.enrage(player);
                }
            }
        }
    }

    private static void spawnBoss(ServerWorld world) {
        if (!active) {
            return;
        }
        var players = world.getPlayers(p -> !p.isSpectator() && p.isAlive());
        if (players.isEmpty()) {
            abort(world);
            return;
        }
        ServerPlayerEntity victim = players.get(world.random.nextInt(players.size()));
        BlockPos pos = HorrorUtil.findGroundSpot(world, victim, 12.0, 20.0, true, world.random);
        if (pos == null) {
            pos = victim.getBlockPos().add(8, 0, 8);
        }
        StalkerEntity boss = ModEntities.STALKER.create(world);
        if (boss == null) {
            abort(world);
            return;
        }
        boss.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        boss.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        boss.makeFinalBoss();
        boss.setCustomName(Text.literal("ОНО").formatted(Formatting.DARK_RED));
        boss.setCustomNameVisible(true);
        if (!world.spawnEntity(boss)) {
            abort(world);
            return;
        }
        boss.enrage(victim);
        bossId = boss.getUuid();
        bossSpawned = true;
        broadcast(world, "Убей — или переживи его терпение.", Formatting.RED, false);
        for (ServerPlayerEntity player : world.getPlayers()) {
            HorrorNetworking.sendCueAt(player, SoundCue.SCREAM, boss.getPos(), 1.4f);
        }
    }

    private static void run(MinecraftServer server, ServerWorld world) {
        if (!worldKey.equals(world.getRegistryKey().getValue().toString())) {
            return;
        }
        if (world.getPlayers().isEmpty()) {
            abort(world);
            return;
        }
        if (bossSpawned) {
            Entity boss = world.getEntity(bossId);
            if (boss == null || !boss.isAlive()) {
                victory(world);
                return;
            }
        }
        if (server.getTicks() - startedAtTick > HUNT_DURATION_TICKS) {
            withdraw(world);
        }
    }

    /** The boss is dead: the world exhales. */
    private static void victory(ServerWorld world) {
        active = false;
        HorrorWorldState state = HorrorWorldState.get(world);
        state.horrorTicks = 0;
        state.nextFinalHuntAt = world.getTime() + RETRY_DELAY_TICKS;
        state.markDirty();
        clearHunters(world);
        world.setWeather(24000, 0, false, false);
        for (ServerPlayerEntity player : world.getPlayers()) {
            state.setSanity(player.getUuid(), 100.0f);
            SanityManager.forceSync(player);
            Exest.eventManager().mood(player, 0.0f, 0.0f, 0, 1);
        }
        broadcast(world, "Оно ушло.", Formatting.GOLD, true);
        broadcast(world, "Пока что.", Formatting.DARK_GRAY, false);
    }

    /** Nobody killed it in time: it loses interest — for a few days. */
    private static void withdraw(ServerWorld world) {
        active = false;
        HorrorWorldState state = HorrorWorldState.get(world);
        state.nextFinalHuntAt = world.getTime() + RETRY_DELAY_TICKS;
        state.markDirty();
        clearHunters(world);
        broadcast(world, "Оно остаётся.", Formatting.DARK_RED, true);
    }

    private static void abort(ServerWorld world) {
        active = false;
        HorrorWorldState state = HorrorWorldState.get(world);
        state.nextFinalHuntAt = world.getTime() + RETRY_DELAY_TICKS;
        state.markDirty();
        clearHunters(world);
    }

    private static void clearHunters(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof AbstractHorrorEntity) {
                entity.discard();
            }
        }
    }

    private static void broadcast(ServerWorld world, String message, Formatting color, boolean bold) {
        Text text = bold
                ? Text.literal(message).formatted(color, Formatting.BOLD)
                : Text.literal(message).formatted(color);
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(text, false);
        }
    }
}
