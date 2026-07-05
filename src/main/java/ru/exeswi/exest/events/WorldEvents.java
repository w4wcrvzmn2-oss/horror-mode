package ru.exeswi.exest.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameRules;
import ru.exeswi.exest.networking.payload.MoodPayload;

import static ru.exeswi.exest.events.HorrorEvent.Category.WORLD;

/**
 * World-scale wrongness, synchronized for everyone in the dimension: red nights,
 * total blackouts, storms that refuse to end, time that stops moving.
 */
public final class WorldEvents {

    private WorldEvents() {
    }

    public static void registerAll() {
        HorrorEvent.builder("red_moon", WORLD).weight(8).cooldown(36000).minDifficulty(2)
                .enabledWhen(c -> c.enableWorldEvents)
                .condition(p -> p.getServerWorld().isNight())
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.35f, 0.25f, MoodPayload.FLAG_RED_MOON, 2400)).register();

        // both are short, sharp shocks — never a sustained "can't see anything" state
        HorrorEvent.builder("blackout", WORLD).weight(6).cooldown(36000).minDifficulty(3)
                .enabledWhen(c -> c.enableWorldEvents && c.enableDarkness)
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.95f, 0.6f, 0, 160)).register();

        HorrorEvent.builder("fog_wall", WORLD).weight(8).cooldown(18000)
                .enabledWhen(c -> c.enableWorldEvents && c.enableDarkness)
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.3f, 0.97f, 0, 240)).register();

        HorrorEvent.builder("endless_thunder", WORLD).weight(6).cooldown(48000).minDifficulty(2)
                .enabledWhen(c -> c.enableWorldEvents)
                .action(WorldEvents::endlessThunder).register();

        HorrorEvent.builder("time_freeze", WORLD).weight(5).cooldown(48000)
                .enabledWhen(c -> c.enableWorldEvents)
                .action(WorldEvents::freezeTime).register();

        HorrorEvent.builder("silence_zone", WORLD).weight(7).cooldown(24000)
                .enabledWhen(c -> c.enableWorldEvents)
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.0f, 0.0f, MoodPayload.FLAG_SILENCE, 700)).register();

        HorrorEvent.builder("compass_spin", WORLD).weight(6).cooldown(24000)
                .enabledWhen(c -> c.enableWorldEvents)
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.0f, 0.0f, MoodPayload.FLAG_COMPASS_SPIN, 1200)).register();

        HorrorEvent.builder("light_flicker", WORLD).weight(9).cooldown(12000)
                .enabledWhen(c -> c.enableWorldEvents && c.enableDarkness)
                .action((p, m) -> m.moodForWorld(p.getServerWorld(),
                        0.0f, 0.0f, MoodPayload.FLAG_LIGHT_FLICKER, 300)).register();
    }

    private static void endlessThunder(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        MinecraftServer server = world.getServer();
        int duration = 2400 + world.random.nextInt(2400);
        world.setWeather(0, duration, true, true);
        // thunder rumbles far more often than any natural storm would allow
        int strikes = duration / 100;
        for (int i = 0; i < strikes; i++) {
            manager.schedule(server, i * 100 + world.random.nextInt(60), () -> {
                for (ServerPlayerEntity target : world.getPlayers()) {
                    world.playSound(null, target.getX() + world.random.nextInt(40) - 20,
                            target.getY() + 20, target.getZ() + world.random.nextInt(40) - 20,
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER,
                            2.0f, 0.6f + world.random.nextFloat() * 0.4f);
                }
            });
        }
    }

    private static void freezeTime(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        MinecraftServer server = world.getServer();
        GameRules.BooleanRule rule = world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE);
        if (!rule.get()) {
            return; // someone froze time already, leave their setting alone
        }
        rule.set(false, server);
        manager.schedule(server, 2400, () -> rule.set(true, server));
    }
}
