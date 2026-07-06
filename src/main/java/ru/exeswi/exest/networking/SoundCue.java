package ru.exeswi.exest.networking;

/**
 * Abstract audio cues. The server only decides *what* should be heard and *where*;
 * the client audio manager translates a cue into concrete (vanilla) sounds with
 * distorted pitch, sequencing and 3D positioning.
 */
public enum SoundCue {
    DISTANT_FOOTSTEPS,
    BREATHING,
    WHISPER,
    UNKNOWN,
    CAVE,
    SCREAM,
    CHILD_LAUGH,
    STATIC_NOISE,
    RADIO,
    REVERSED,
    CRYING,
    HEARTBEAT,
    /** Loud, sharp jumpscare sting. */
    STING,
    /**
     * Overlapping voices pressing on the mind (custom stereo recording — different
     * voices in each ear with headphones). Always played non-positionally.
     */
    VOICES,
    /** Something heavy nearing the house (custom stereo recording, non-positional). */
    APPROACH;

    private static final SoundCue[] VALUES = values();

    public static SoundCue byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : UNKNOWN;
    }
}
