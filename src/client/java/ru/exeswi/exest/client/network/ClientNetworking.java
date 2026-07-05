package ru.exeswi.exest.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ru.exeswi.exest.client.audio.ClientAudioManager;
import ru.exeswi.exest.client.hallucination.Hallucinations;
import ru.exeswi.exest.client.screen.FakeCrashScreen;
import ru.exeswi.exest.client.screen.FakeLoadingScreen;
import ru.exeswi.exest.client.screen.FakeTitleScreen;
import ru.exeswi.exest.client.state.ClientHorrorState;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.networking.payload.EffectPayload;
import ru.exeswi.exest.networking.payload.FakeUiPayload;
import ru.exeswi.exest.networking.payload.MoodPayload;
import ru.exeswi.exest.networking.payload.SanityPayload;
import ru.exeswi.exest.networking.payload.SoundCuePayload;

/** Receives every horror payload and routes it to the right client subsystem. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.ID, (payload, context) ->
                context.client().execute(() -> handleEffect(context.client(), payload)));

        ClientPlayNetworking.registerGlobalReceiver(SoundCuePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientAudioManager.playCue(context.client(), payload)));

        ClientPlayNetworking.registerGlobalReceiver(MoodPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientHorrorState.applyMood(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SanityPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientHorrorState.sanity = payload.sanity();
                    ClientHorrorState.horrorLevel = payload.horrorLevel();
                }));

        ClientPlayNetworking.registerGlobalReceiver(FakeUiPayload.ID, (payload, context) ->
                context.client().execute(() -> handleFakeUi(context.client(), payload)));
    }

    private static void handleEffect(MinecraftClient client, EffectPayload payload) {
        float intensity = payload.intensity();
        int duration = payload.durationTicks();
        switch (payload.effect()) {
            case FLASH -> ClientHorrorState.flash(duration);
            case GLITCH -> ClientHorrorState.glitch(intensity, duration);
            case STATIC -> ClientHorrorState.staticBurst(intensity, duration);
            case SCREEN_RUNNER -> ClientHorrorState.screenRunner(duration);
            case BLINK -> ClientHorrorState.blink(duration);
            case SHAKE -> ClientHorrorState.shake(intensity, duration);
            case HALLUCINATION_OVERLAY -> ClientHorrorState.hallucinationOverlay(duration);
            case FAKE_ORE -> Hallucinations.spawnFakeOre(client, duration);
            case FAKE_MOB -> Hallucinations.spawnFakeMob(client, duration);
            case FAKE_PLAYER -> Hallucinations.spawnFakePlayer(client, duration);
            case MUSIC_DISTORT -> ClientAudioManager.distortMusic(client);
            case JUMPSCARE_FACE -> ClientHorrorState.jumpscareFace(duration);
        }
    }

    private static void handleFakeUi(MinecraftClient client, FakeUiPayload payload) {
        // never interrupt a real menu the player is using
        if (client.currentScreen != null || !ConfigManager.get().enableJumpscares) {
            return;
        }
        switch (payload.type()) {
            case CRASH -> client.setScreen(new FakeCrashScreen());
            case LOADING -> client.setScreen(new FakeLoadingScreen());
            case TITLE -> client.setScreen(new FakeTitleScreen());
        }
    }
}
