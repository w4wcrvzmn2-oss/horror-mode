package ru.exeswi.exest.client.handler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;
import ru.exeswi.exest.client.audio.ClientAudioManager;
import ru.exeswi.exest.client.hallucination.Hallucinations;
import ru.exeswi.exest.client.state.ClientHorrorState;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.networking.SoundCue;

/**
 * Per-tick client work: effect timers, camera shake and sanity-driven local
 * hallucinations (drifting camera, breathing behind you, involuntary blinks) that
 * need no server round-trip.
 */
public final class ClientTickHandler {

    private static final Random RANDOM = Random.create();

    private static int driftTicks;
    private static float driftYawPerTick;

    private ClientTickHandler() {
    }

    public static void tick(MinecraftClient client) {
        ClientHorrorState.tick(client);
        ClientAudioManager.tick(client);
        Hallucinations.tick(client);

        if (client.player == null || client.world == null || client.isPaused()) {
            return;
        }
        tickCamera(client);
        tickSanityAmbience(client);
    }

    private static void tickCamera(MinecraftClient client) {
        if (!ConfigManager.get().enableVisualEffects) {
            return;
        }
        // shake bursts: sharp, random, short
        if (ClientHorrorState.shakeTicks > 0) {
            float power = ClientHorrorState.shakeIntensity;
            client.player.setYaw(client.player.getYaw() + (RANDOM.nextFloat() - 0.5f) * 3.0f * power);
            client.player.setPitch(client.player.getPitch() + (RANDOM.nextFloat() - 0.5f) * 1.5f * power);
        }
        // sanity drift: the view slowly slides on its own, barely noticeable
        if (ClientHorrorState.sanity < 35.0f && ConfigManager.get().enableHallucinations) {
            if (driftTicks <= 0 && RANDOM.nextInt(300) == 0) {
                driftTicks = 30 + RANDOM.nextInt(50);
                driftYawPerTick = (RANDOM.nextFloat() - 0.5f) * 0.14f;
            }
        }
        if (driftTicks > 0) {
            driftTicks--;
            client.player.setYaw(client.player.getYaw() + driftYawPerTick);
        }
    }

    private static void tickSanityAmbience(MinecraftClient client) {
        if (!ConfigManager.get().enableHallucinations || ClientHorrorState.sanity >= 45.0f) {
            return;
        }
        float fear = (45.0f - ClientHorrorState.sanity) / 45.0f;
        // the lower the sanity, the more the mind produces on its own
        if (RANDOM.nextInt(1400) < (int) (fear * 14) + 1) {
            SoundCue cue = switch (RANDOM.nextInt(4)) {
                case 0 -> SoundCue.BREATHING;
                case 1 -> SoundCue.WHISPER;
                case 2 -> SoundCue.HEARTBEAT;
                default -> SoundCue.DISTANT_FOOTSTEPS;
            };
            ClientAudioManager.localCue(client, cue,
                    0.4f * (float) ConfigManager.get().audioIntensity);
        }
        // involuntary blinks when the mind is nearly gone
        if (ClientHorrorState.sanity < 15.0f && RANDOM.nextInt(600) == 0) {
            ClientHorrorState.blink(20);
        }
    }
}
