package ru.exeswi.exest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.exeswi.exest.ai.PlayerBehaviorTracker;
import ru.exeswi.exest.command.HorrorCommand;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.events.AbductionSequence;
import ru.exeswi.exest.events.AmbientEvents;
import ru.exeswi.exest.events.EncounterEvents;
import ru.exeswi.exest.events.FakeMessages;
import ru.exeswi.exest.events.HorrorEventManager;
import ru.exeswi.exest.events.RareEvents;
import ru.exeswi.exest.events.WorldEvents;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.registry.ModEntities;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.world.CorruptionManager;

/**
 * Horror Mode: a psychological horror conversion. The server side orchestrates
 * everything — events, monsters, sanity, world decay — while the client side only
 * renders what it is told to feel.
 */
public class Exest implements ModInitializer {

    public static final String MOD_ID = "exest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HorrorEventManager eventManager;

    @Override
    public void onInitialize() {
        ConfigManager.load();
        HorrorNetworking.register();
        ModEntities.registerAttributes();
        ru.exeswi.exest.registry.ModSounds.init();

        AmbientEvents.registerAll();
        EncounterEvents.registerAll();
        FakeMessages.registerAll();
        RareEvents.registerAll();
        WorldEvents.registerAll();
        ru.exeswi.exest.events.StructureEvents.registerAll();
        ru.exeswi.exest.events.JournalEvents.registerAll();

        eventManager = new HorrorEventManager();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            eventManager.tick(server);
            AbductionSequence.tick(server);
            CorruptionManager.tick(server);
            ru.exeswi.exest.events.ActivityLog.tick(server);
            if (server.getTicks() % 20 == 0) {
                SanityManager.tick(server);
                PlayerBehaviorTracker.tick(server);
                ru.exeswi.exest.events.JournalEvents.tick(server);
                for (var player : server.getPlayerManager().getPlayerList()) {
                    if (!player.isSpectator()) {
                        ru.exeswi.exest.events.StructureEvents.checkAltars(player);
                    }
                }
            }
            if (server.getTicks() % 80 == 0) {
                ru.exeswi.exest.events.PresenceRadar.tick(server);
            }
            if (server.getTicks() % 100 == 0) {
                ru.exeswi.exest.events.FinalHunt.tick(server);
                ru.exeswi.exest.events.FirstNight.tick(server);
            }
        });

        // the journal needs to know what you were doing; it counts your broken blocks
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
                (world, breaker, pos, state, blockEntity) -> {
                    if (breaker instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                        ru.exeswi.exest.events.ActivityLog.onBlockBroken(serverPlayer);
                    }
                });

        // deaths at its hands are part of the score it keeps
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player
                            && damageSource.getAttacker() instanceof ru.exeswi.exest.entity.base.AbstractHorrorEntity) {
                        ru.exeswi.exest.stats.HorrorStats.inc(player,
                                ru.exeswi.exest.stats.HorrorStats.Stat.DEATHS);
                    }
                });

        // late joiners need their sanity mirrored before effects can scale correctly —
        // and entering the world is exactly when the abduction may choose to happen
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SanityManager.forceSync(handler.getPlayer());
            sendDisclaimerOnce(handler.getPlayer());
            ru.exeswi.exest.events.JournalEvents.restoreBook(handler.getPlayer());
            AbductionSequence.maybeSchedule(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                AbductionSequence.onDisconnect(handler.getPlayer()));
        // death resets nothing: respawning is exactly when it may already be waiting.
        // The mind, however, gets partially wiped — you come back at 50 sanity minimum,
        // otherwise a death at zero would lock the player in a permanent panic spiral
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            var worldState = ru.exeswi.exest.world.HorrorWorldState.get(newPlayer.getServerWorld());
            worldState.setSanity(newPlayer.getUuid(),
                    Math.max(50.0f, worldState.getSanity(newPlayer.getUuid())));
            SanityManager.forceSync(newPlayer);
            // the journal survives death — it comes back with everything it wrote
            ru.exeswi.exest.events.JournalEvents.restoreBook(newPlayer);
            AbductionSequence.maybeSchedule(newPlayer);
            if (newPlayer.getRandom().nextFloat() < 0.4f) {
                String eventId = switch (newPlayer.getRandom().nextInt(3)) {
                    case 0 -> "behind_you";
                    case 1 -> "apparition_distance";
                    default -> "whisper";
                };
                eventManager.schedule(newPlayer.getServer(),
                        400 + newPlayer.getRandom().nextInt(800), () -> {
                    var event = HorrorEventManager.byId(eventId);
                    if (event != null && newPlayer.isAlive() && !newPlayer.isDisconnected()) {
                        eventManager.runNow(event, newPlayer);
                    }
                });
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                HorrorCommand.register(dispatcher));

        LOGGER.info("Horror Mode initialized. It knows you are here.");
    }

    /** Shown once per player per world; the flag is persisted in the world state. */
    private static void sendDisclaimerOnce(net.minecraft.server.network.ServerPlayerEntity player) {
        if (!ru.exeswi.exest.world.HorrorWorldState.get(player.getServerWorld())
                .markDisclaimerSeen(player.getUuid())) {
            return;
        }
        var red = net.minecraft.util.Formatting.DARK_RED;
        var gray = net.minecraft.util.Formatting.GRAY;
        player.sendMessage(net.minecraft.text.Text.literal("⚠ HORROR MODE ⚠")
                .formatted(red, net.minecraft.util.Formatting.BOLD), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "Этот мод содержит скримеры, вспышки, резкие громкие звуки, погони и психологический хоррор.")
                .formatted(gray), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "Не рекомендуется при фоточувствительной эпилепсии и проблемах с сердцем.")
                .formatted(gray), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "This mod contains jumpscares, flashing lights and loud sounds. Player discretion is advised.")
                .formatted(net.minecraft.util.Formatting.DARK_GRAY, net.minecraft.util.Formatting.ITALIC), false);
        player.sendMessage(net.minecraft.text.Text.literal(
                "Настройки: config/exest-horror.json · статус: /horror status · панель: клавиша H")
                .formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
        player.sendMessage(net.minecraft.text.Text.literal("Оно уже знает, что ты здесь.")
                .formatted(red, net.minecraft.util.Formatting.ITALIC), false);
    }

    public static HorrorEventManager eventManager() {
        return eventManager;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
