package ru.exeswi.exest.client.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.networking.payload.MoodPayload;

/**
 * Client-side mirror of everything the horror engine wants the player to feel right
 * now: darkness level, fog density, sustained mood flags and one-shot effect timers.
 * All values are ticked and smoothed here; renderers and mixins only read.
 */
public final class ClientHorrorState {

    private static final Random RANDOM = Random.create();

    /** Synced hidden sanity, 0..100. */
    public static float sanity = 100.0f;
    /** Synced world horror level, 0..10. */
    public static int horrorLevel;
    /** The info panel in the corner, toggled with the HUD key (default H). */
    public static boolean hudVisible = true;

    // sustained mood
    private static float eventDarkness;
    private static int darknessTicks;
    private static float fogTarget;
    private static int fogTicks;
    private static int redMoonTicks;
    private static int silenceTicks;
    private static int compassSpinTicks;
    private static int flickerTicks;

    // smoothed values (the "eye")
    private static float darknessCurrent;
    private static float fogCurrent;
    private static float flickerNoise;

    // one-shot effects, all simple tick countdowns
    public static int flashTicks;
    public static int glitchTicks;
    public static int staticTicks;
    public static int blinkTicks;
    public static int blinkDuration = 1;
    public static int shakeTicks;
    public static float shakeIntensity;
    public static int runnerTicks;
    public static int runnerDuration = 1;
    public static boolean runnerFromLeft;
    public static int overlayTicks;
    public static int overlayDuration = 1;
    public static int faceTicks;
    public static int faceDuration = 1;
    public static float staticIntensity;
    public static float glitchIntensity;

    private ClientHorrorState() {
    }

    public static void applyMood(MoodPayload payload) {
        if (payload.darkness() > 0.0f && ConfigManager.get().enableDarkness) {
            eventDarkness = payload.darkness();
            darknessTicks = payload.durationTicks();
        }
        if (payload.fog() > 0.0f && ConfigManager.get().enableDarkness) {
            fogTarget = payload.fog();
            fogTicks = payload.durationTicks();
        }
        if (payload.has(MoodPayload.FLAG_RED_MOON)) {
            redMoonTicks = payload.durationTicks();
        }
        if (payload.has(MoodPayload.FLAG_SILENCE)) {
            silenceTicks = payload.durationTicks();
        }
        if (payload.has(MoodPayload.FLAG_COMPASS_SPIN)) {
            compassSpinTicks = payload.durationTicks();
        }
        if (payload.has(MoodPayload.FLAG_LIGHT_FLICKER)) {
            flickerTicks = payload.durationTicks();
        }
    }

    public static void tick(MinecraftClient client) {
        darknessTicks = decrement(darknessTicks);
        fogTicks = decrement(fogTicks);
        redMoonTicks = decrement(redMoonTicks);
        silenceTicks = decrement(silenceTicks);
        compassSpinTicks = decrement(compassSpinTicks);
        flickerTicks = decrement(flickerTicks);
        flashTicks = decrement(flashTicks);
        glitchTicks = decrement(glitchTicks);
        staticTicks = decrement(staticTicks);
        blinkTicks = decrement(blinkTicks);
        shakeTicks = decrement(shakeTicks);
        runnerTicks = decrement(runnerTicks);
        overlayTicks = decrement(overlayTicks);
        faceTicks = decrement(faceTicks);

        if (flickerTicks > 0 && RANDOM.nextInt(3) == 0) {
            flickerNoise = RANDOM.nextFloat() * 0.5f;
        } else if (flickerTicks <= 0) {
            flickerNoise = 0.0f;
        }

        // darkness slams in hard and lets go noticeably slower — but it is NEVER
        // permanent: outside of events the world looks like vanilla
        float target = targetDarkness(client);
        float rate = target > darknessCurrent ? 0.35f : 0.06f;
        darknessCurrent += (target - darknessCurrent) * rate;
        if (darknessCurrent < 0.01f) {
            darknessCurrent = 0.0f;
        }

        float fogGoal = fogTicks > 0 ? fogTarget : 0.0f;
        fogCurrent += (fogGoal - fogCurrent) * 0.05f;
    }

    private static float targetDarkness(MinecraftClient client) {
        if (!ConfigManager.get().enableDarkness || client.world == null || client.player == null) {
            return 0.0f;
        }
        float target = darknessTicks > 0 ? eventDarkness : 0.0f;
        target += flickerNoise;
        return MathHelper.clamp(target, 0.0f, 0.95f);
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    // --- read access for mixins and renderers ---

    /** Extra lightmap darkness 0..1, consumed by the lightmap mixin. */
    public static float darknessFactor() {
        return darknessCurrent;
    }

    /** Fog density 0..1, consumed by the fog mixin. */
    public static float fogFactor() {
        return fogCurrent;
    }

    /** Multiplier for night vision strength while horror darkness is enabled. */
    public static float nightVisionFactor() {
        return ConfigManager.get().enableDarkness ? 0.25f : 1.0f;
    }

    public static boolean isSilenced() {
        return silenceTicks > 0;
    }

    public static boolean isCompassSpinning() {
        return compassSpinTicks > 0;
    }

    public static boolean isRedMoon() {
        return redMoonTicks > 0;
    }

    // --- one-shot triggers ---

    public static void flash(int duration) {
        flashTicks = duration;
    }

    public static void glitch(float intensity, int duration) {
        glitchIntensity = intensity;
        glitchTicks = duration;
    }

    public static void staticBurst(float intensity, int duration) {
        staticIntensity = intensity;
        staticTicks = duration;
    }

    public static void blink(int duration) {
        blinkTicks = duration;
        blinkDuration = Math.max(1, duration);
    }

    public static void shake(float intensity, int duration) {
        shakeIntensity = intensity;
        shakeTicks = duration;
    }

    public static void screenRunner(int duration) {
        runnerTicks = duration;
        runnerDuration = Math.max(1, duration);
        runnerFromLeft = RANDOM.nextBoolean();
    }

    public static void hallucinationOverlay(int duration) {
        overlayTicks = duration;
        overlayDuration = Math.max(1, duration);
    }

    public static void jumpscareFace(int duration) {
        faceTicks = duration;
        faceDuration = Math.max(1, duration);
    }

    public static Random random() {
        return RANDOM;
    }
}
