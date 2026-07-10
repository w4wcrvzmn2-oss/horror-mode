package ru.exeswi.exest.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.config.HorrorConfig;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.networking.FakeUi;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.WeightedPicker;
import ru.exeswi.exest.world.HorrorWorldState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The heart of the mod: a weighted, cooldown-aware scheduler that decides when each
 * player gets scared and how. Three independent timers run per player (frequent
 * ambient/encounter/fake events, very rare events) and per world (world-wide events).
 * All timers shrink with difficulty and low sanity. Runs cheap checks once a second.
 */
public final class HorrorEventManager {

    private static final Map<String, HorrorEvent> EVENTS = new LinkedHashMap<>();

    private final Map<UUID, Integer> mainTimer = new HashMap<>();
    private final Map<UUID, Integer> rareTimer = new HashMap<>();
    private final Map<UUID, Integer> burstLeft = new HashMap<>();
    private final Map<String, Integer> worldTimer = new HashMap<>();
    private final Map<String, Long> cooldownUntil = new HashMap<>();
    private final List<ScheduledTask> tasks = new ArrayList<>();

    private record ScheduledTask(long fireAtTick, Runnable action) {
    }

    public static void register(HorrorEvent event) {
        EVENTS.put(event.id(), event);
    }

    public static Set<String> eventIds() {
        return EVENTS.keySet();
    }

    public static HorrorEvent byId(String id) {
        return EVENTS.get(id);
    }

    public void tick(MinecraftServer server) {
        if (!server.getPlayerManager().getPlayerList().isEmpty()) {
            HorrorWorldState state = HorrorWorldState.get(server.getOverworld());
            state.horrorTicks++;
            if (state.horrorTicks % 200 == 0) {
                state.markDirty();
            }
        }
        runScheduled(server);
        if (server.getTicks() % 20 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            tickPlayerTimers(player);
        }
        for (ServerWorld world : server.getWorlds()) {
            if (!world.getPlayers().isEmpty()) {
                tickWorldTimer(world);
            }
        }
    }

    private void tickPlayerTimers(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (!mainTimer.containsKey(id)) {
            // fresh player: the first "did you hear that?" comes within 30-90 seconds,
            // rare events wait their proper turn
            mainTimer.put(id, 600 + player.getRandom().nextInt(1200));
            rareTimer.put(id, rollInterval(player, 18000, 36000));
            return;
        }
        int main = mainTimer.merge(id, -20, Integer::sum);
        if (main <= 0) {
            fire(player, EnumSet.of(HorrorEvent.Category.AMBIENT,
                    HorrorEvent.Category.ENCOUNTER, HorrorEvent.Category.FAKE));
            // wave pacing: long unnerving lulls broken by dense bursts of 2-5 events.
            // silence does half the frightening on its own
            int left = burstLeft.getOrDefault(id, 0);
            if (left > 0) {
                burstLeft.put(id, left - 1);
                mainTimer.put(id, 120 + player.getRandom().nextInt(280));
            } else if (player.getRandom().nextFloat() < 0.35f) {
                burstLeft.put(id, 1 + player.getRandom().nextInt(3));
                mainTimer.put(id, 140 + player.getRandom().nextInt(240));
            } else {
                mainTimer.put(id, rollInterval(player, 4800, 12000));
            }
        }

        int rare = rareTimer.merge(id, -20, Integer::sum);
        if (rare <= 0) {
            rareTimer.put(id, rollInterval(player, 18000, 36000));
            fire(player, EnumSet.of(HorrorEvent.Category.RARE));
        }
    }

    private void tickWorldTimer(ServerWorld world) {
        String key = world.getRegistryKey().getValue().toString();
        int left = worldTimer.merge(key, -20, Integer::sum);
        if (left <= 0) {
            List<ServerPlayerEntity> players = world.getPlayers(p -> !p.isSpectator() && p.isAlive());
            if (!players.isEmpty()) {
                ServerPlayerEntity anchor = players.get(world.random.nextInt(players.size()));
                worldTimer.put(key, rollInterval(anchor, 12000, 24000));
                fire(anchor, EnumSet.of(HorrorEvent.Category.WORLD));
            } else {
                worldTimer.put(key, 12000);
            }
        }
    }

    private int rollInterval(ServerPlayerEntity player, int min, int max) {
        HorrorConfig config = ConfigManager.get();
        double freq = Math.max(0.05, config.eventFrequency)
                * DifficultyScaler.frequencyMultiplier(player.getServerWorld());
        if (SanityManager.isLow(player)) {
            freq *= 1.8; // a fraying mind attracts attention
        }
        if (config.debugMode) {
            freq *= 10.0;
        }
        int base = min + player.getRandom().nextInt(Math.max(1, max - min));
        return Math.max(100, (int) (base / freq));
    }

    /** Picks and runs one eligible event of the given categories for this player. */
    public void fire(ServerPlayerEntity player, Set<HorrorEvent.Category> categories) {
        HorrorConfig config = ConfigManager.get();
        long now = player.getServerWorld().getTime();
        int difficulty = DifficultyScaler.level(player.getServerWorld());

        List<HorrorEvent> pool = new ArrayList<>();
        for (HorrorEvent event : EVENTS.values()) {
            if (!categories.contains(event.category())
                    || !event.isEnabled(config)
                    || event.minDifficulty() > difficulty
                    || cooldownUntil.getOrDefault(event.id(), 0L) > now) {
                continue;
            }
            try {
                if (event.canRun(player)) {
                    pool.add(event);
                }
            } catch (Exception e) {
                Exest.LOGGER.error("Horror event {} condition failed", event.id(), e);
            }
        }
        HorrorEvent picked = WeightedPicker.pick(pool, HorrorEvent::weight, player.getRandom());
        if (picked == null) {
            return;
        }
        runNow(picked, player);
    }

    /** Runs a specific event, bypassing timers but recording its cooldown. Used by /horror event. */
    public void runNow(HorrorEvent event, ServerPlayerEntity player) {
        cooldownUntil.put(event.id(), player.getServerWorld().getTime() + event.cooldownTicks());
        if (ConfigManager.get().debugMode) {
            Exest.LOGGER.info("[horror] event '{}' -> {}", event.id(), player.getGameProfile().getName());
        }
        try {
            event.run(player, this);
            ru.exeswi.exest.stats.HorrorStats.inc(player, ru.exeswi.exest.stats.HorrorStats.Stat.EVENTS);
        } catch (Exception e) {
            Exest.LOGGER.error("Horror event {} failed", event.id(), e);
        }
    }

    // --- helpers available to event actions ---

    public void effect(ServerPlayerEntity player, HorrorEffect effect, float intensity, int duration) {
        HorrorNetworking.sendEffect(player, effect, intensity, duration);
    }

    public void cueAt(ServerPlayerEntity player, SoundCue cue, Vec3d pos, float volume) {
        HorrorNetworking.sendCueAt(player, cue, pos, (float) (volume * ConfigManager.get().audioIntensity));
    }

    /**
     * Co-op moments: everyone close enough hears the same thing from the same spot,
     * so the whole group turns their heads at once.
     */
    public void cueForNearby(ServerWorld world, Vec3d pos, SoundCue cue, float volume, double radius) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && player.getPos().squaredDistanceTo(pos) < radius * radius) {
                cueAt(player, cue, pos, volume);
            }
        }
    }

    public void cueBehind(ServerPlayerEntity player, SoundCue cue, float volume) {
        HorrorNetworking.sendCueBehind(player, cue, (float) (volume * ConfigManager.get().audioIntensity));
    }

    public void mood(ServerPlayerEntity player, float darkness, float fog, int flags, int duration) {
        HorrorNetworking.sendMood(player, darkness, fog, flags, duration);
    }

    public void moodForWorld(ServerWorld world, float darkness, float fog, int flags, int duration) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            mood(player, darkness, fog, flags, duration);
        }
    }

    public void fakeUi(ServerPlayerEntity player, FakeUi ui) {
        HorrorNetworking.sendFakeUi(player, ui);
    }

    /**
     * Kills every sound in the world for a moment, then runs the scare. The brain
     * registers sudden total silence before it registers any threat — by the time the
     * sting lands, the player is already tense.
     */
    public void silenceThen(ServerPlayerEntity player, int silenceTicks, Runnable scare) {
        mood(player, 0.0f, 0.0f, ru.exeswi.exest.networking.payload.MoodPayload.FLAG_SILENCE, silenceTicks);
        schedule(player.getServer(), silenceTicks + 5, () -> {
            if (player.isAlive() && !player.isDisconnected()) {
                scare.run();
            }
        });
    }

    /** Schedules a follow-up action on the server thread, delay in ticks. */
    public void schedule(MinecraftServer server, int delayTicks, Runnable action) {
        tasks.add(new ScheduledTask(server.getTicks() + delayTicks, action));
    }

    private void runScheduled(MinecraftServer server) {
        if (tasks.isEmpty()) {
            return;
        }
        long now = server.getTicks();
        Iterator<ScheduledTask> it = tasks.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            if (task.fireAtTick <= now) {
                it.remove();
                try {
                    task.action.run();
                } catch (Exception e) {
                    Exest.LOGGER.error("Scheduled horror task failed", e);
                }
            }
        }
    }
}
