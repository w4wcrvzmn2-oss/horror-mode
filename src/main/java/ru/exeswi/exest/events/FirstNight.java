package ru.exeswi.exest.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.world.HorrorWorldState;

/**
 * The first night of a world is never allowed to pass quietly. Once, and exactly
 * once: the world falls dead silent, the voices crawl into both ears, the light
 * dies for a few seconds — and when it comes back, a figure is standing in plain
 * view, watching. Horror level zero means nothing. It was always here.
 */
public final class FirstNight {

    private FirstNight() {
    }

    /** Called every 100 ticks from the main loop. */
    public static void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        HorrorWorldState state = HorrorWorldState.get(world);
        if (state.firstNightDone || !world.isNight() || world.getPlayers().isEmpty()
                || !ConfigManager.get().enableMonsters) {
            return;
        }
        state.firstNightDone = true;
        state.markDirty();
        HorrorEventManager manager = Exest.eventManager();
        for (ServerPlayerEntity player : world.getPlayers(p -> !p.isSpectator() && p.isAlive())) {
            manager.schedule(server, 100 + player.getRandom().nextInt(200), () -> {
                if (!player.isAlive() || player.isDisconnected()) {
                    return;
                }
                manager.silenceThen(player, 70, () -> welcome(player, manager));
            });
        }
    }

    private static void welcome(ServerPlayerEntity player, HorrorEventManager manager) {
        HorrorNetworking.sendCueBehind(player, SoundCue.VOICES,
                (float) ConfigManager.get().audioIntensity);
        manager.mood(player, 0.85f, 0.3f, 0, 140);
        var ghost = ApparitionSpawner.spawnApparition(player, ApparitionSpawner.Placement.IN_VIEW);
        if (ghost == null) {
            ghost = ApparitionSpawner.spawnApparition(player, ApparitionSpawner.Placement.DISTANCE);
        }
        if (ghost != null) {
            HorrorNetworking.sendCueAt(player, SoundCue.STING, ghost.getPos(), 1.0f);
        }
        SanityManager.modify(player, -12.0f);
    }
}
