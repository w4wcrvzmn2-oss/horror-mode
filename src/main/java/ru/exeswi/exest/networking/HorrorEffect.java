package ru.exeswi.exest.networking;

/**
 * One-shot client-side effects triggered by the server. Sent by ordinal inside
 * {@link ru.exeswi.exest.networking.payload.EffectPayload}.
 */
public enum HorrorEffect {
    /** Whole screen flashes white for a few frames. */
    FLASH,
    /** Screen glitch burst: RGB fringing, torn rows, block artifacts. */
    GLITCH,
    /** TV-static burst over the whole screen. */
    STATIC,
    /** A black humanoid silhouette sprints across the screen. */
    SCREEN_RUNNER,
    /** Eyelid-style blink: black bars close and reopen. */
    BLINK,
    /** Camera shake burst. */
    SHAKE,
    /** Faint dark hallucination overlay (watching eyes). */
    HALLUCINATION_OVERLAY,
    /** Client-side fake ore appears in nearby stone. */
    FAKE_ORE,
    /** Client-side fake mob appears nearby and later vanishes. */
    FAKE_MOB,
    /** Client-side fake player appears in the distance. */
    FAKE_PLAYER,
    /** Music becomes distorted: current track stops, a slowed record plays. */
    MUSIC_DISTORT,
    /** Fullscreen screaming face, shaking and torn. Used by the abduction sequence. */
    JUMPSCARE_FACE;

    private static final HorrorEffect[] VALUES = values();

    public static HorrorEffect byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : FLASH;
    }
}
