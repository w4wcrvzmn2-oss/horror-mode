package ru.exeswi.exest.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import ru.exeswi.exest.Exest;

/**
 * The mod's own recorded sounds. {@code VOICESTART} is a stereo recording — different
 * voices in each ear — so it must always be played non-positionally: 3D attenuation
 * would collapse the channel separation that makes it work in headphones.
 */
public final class ModSounds {

    /** Something heavy approaching the house. */
    public static final SoundEvent FALL = register("fall");
    /** Overlapping voices pressing on the mind; full stereo, headphones recommended. */
    public static final SoundEvent VOICESTART = register("voicestart");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        return Registry.register(Registries.SOUND_EVENT, Exest.id(name), SoundEvent.of(Exest.id(name)));
    }

    public static void init() {
        // touching the class registers the constants
    }
}
