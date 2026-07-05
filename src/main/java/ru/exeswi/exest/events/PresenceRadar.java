package ru.exeswi.exest.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.util.HorrorUtil;

/**
 * The heartbeat radar: when a real hunter is nearby but NOT on screen, the player's
 * heart starts pounding — louder the closer it is. No visuals, no message, just the
 * body knowing something is there before the eyes do. Seeing the monster silences
 * the heart: dread lives in the unseen.
 */
public final class PresenceRadar {

    private static final double RANGE = 24.0;

    private PresenceRadar() {
    }

    /** Called every 80 ticks — one heartbeat sequence fits neatly in the gap. */
    public static void tick(MinecraftServer server) {
        if (!ConfigManager.get().enableMonsters || ConfigManager.get().audioIntensity <= 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            pulse(player);
        }
    }

    private static void pulse(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        AbstractHorrorEntity nearest = null;
        double nearestSq = RANGE * RANGE;
        for (AbstractHorrorEntity mob : world.getEntitiesByClass(AbstractHorrorEntity.class,
                Box.of(player.getPos(), RANGE * 2, RANGE, RANGE * 2), e -> !e.isApparition())) {
            double distSq = mob.squaredDistanceTo(player);
            if (distSq < nearestSq) {
                nearest = mob;
                nearestSq = distSq;
            }
        }
        if (nearest == null || HorrorUtil.isInViewCone(player, nearest.getBoundingBox().getCenter())) {
            return;
        }
        float closeness = 1.0f - (float) (Math.sqrt(nearestSq) / RANGE);
        float volume = (0.3f + closeness * 0.8f) * (float) ConfigManager.get().audioIntensity;
        HorrorNetworking.sendCueBehind(player, SoundCue.HEARTBEAT, volume);
        // ...and if you can hear your heart, so can it
        nearest.onHeartbeatHeard(player);
    }
}
