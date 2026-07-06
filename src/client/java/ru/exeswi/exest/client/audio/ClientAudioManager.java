package ru.exeswi.exest.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.networking.payload.SoundCuePayload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Translates abstract {@link SoundCue}s into concrete 3D-positioned vanilla sounds with
 * distorted pitch and multi-step sequencing (footsteps that approach, breaths that
 * repeat, heartbeats that refuse to stop). No custom audio files needed: everything is
 * vanilla material played wrong.
 */
public final class ClientAudioManager {

    private record Pending(int ticksLeft, SoundEvent sound, Vec3d pos, float volume, float pitch) {
    }

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final Random RANDOM = Random.create();

    private ClientAudioManager() {
    }

    public static void playCue(MinecraftClient client, SoundCuePayload payload) {
        if (client.player == null || client.world == null) {
            return;
        }
        Vec3d pos = payload.relative()
                ? behindPlayer(client)
                : new Vec3d(payload.x(), payload.y(), payload.z());
        float volume = payload.volume();

        switch (payload.cue()) {
            case DISTANT_FOOTSTEPS -> {
                int steps = 4 + RANDOM.nextInt(5);
                for (int i = 0; i < steps; i++) {
                    // each step lands a little closer than the last
                    Vec3d stepPos = pos.lerp(client.player.getPos(), i * 0.06);
                    schedule(i * 7, SoundEvents.BLOCK_GRAVEL_STEP, stepPos, volume, 0.7f + RANDOM.nextFloat() * 0.2f);
                }
            }
            case BREATHING -> {
                for (int i = 0; i < 3; i++) {
                    schedule(i * 25, SoundEvents.ENTITY_WARDEN_SNIFF, pos, volume, 0.45f + RANDOM.nextFloat() * 0.1f);
                }
            }
            case WHISPER -> schedule(0, RANDOM.nextBoolean()
                    ? SoundEvents.ENTITY_ENDERMAN_AMBIENT
                    : SoundEvents.ENTITY_WARDEN_LISTENING, pos, volume, 0.5f + RANDOM.nextFloat() * 0.15f);
            case UNKNOWN -> schedule(0, switch (RANDOM.nextInt(3)) {
                case 0 -> SoundEvents.BLOCK_SCULK_SENSOR_CLICKING;
                case 1 -> SoundEvents.ENTITY_ENDERMAN_STARE;
                default -> SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE;
            }, pos, volume, 0.4f + RANDOM.nextFloat() * 0.3f);
            case CAVE -> schedule(0, SoundEvents.AMBIENT_CAVE.value(), pos, volume, 0.8f + RANDOM.nextFloat() * 0.3f);
            case SCREAM -> schedule(0, RANDOM.nextBoolean()
                    ? SoundEvents.ENTITY_ENDERMAN_SCREAM
                    : SoundEvents.ENTITY_GHAST_HURT, pos, volume, 0.45f + RANDOM.nextFloat() * 0.2f);
            case CHILD_LAUGH -> {
                schedule(0, SoundEvents.ENTITY_WITCH_CELEBRATE, pos, volume, 1.7f);
                schedule(18, SoundEvents.ENTITY_VILLAGER_AMBIENT, pos, volume * 0.8f, 2.0f);
            }
            case STATIC_NOISE -> {
                for (int i = 0; i < 4; i++) {
                    schedule(i * 5, SoundEvents.WEATHER_RAIN, pos, volume * 0.5f, 1.9f + RANDOM.nextFloat() * 0.2f);
                }
            }
            case RADIO -> {
                schedule(0, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, pos, volume, 1.8f);
                schedule(8, SoundEvents.WEATHER_RAIN, pos, volume * 0.4f, 2.0f);
                schedule(20, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, pos, volume, 0.6f);
            }
            case REVERSED -> schedule(0, SoundEvents.ENTITY_GHAST_AMBIENT, pos, volume, 0.25f);
            case CRYING -> {
                schedule(0, SoundEvents.ENTITY_GHAST_AMBIENT, pos, volume, 0.7f);
                schedule(35, SoundEvents.ENTITY_GHAST_AMBIENT, pos, volume * 0.8f, 0.65f);
            }
            case HEARTBEAT -> {
                for (int i = 0; i < 5; i++) {
                    schedule(i * 14, SoundEvents.ENTITY_WARDEN_HEARTBEAT, pos, volume, 1.0f);
                }
            }
            case STING -> {
                // the one sound that is allowed to be loud
                schedule(0, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, pos, volume * 1.4f, 0.75f);
                schedule(3, SoundEvents.ENTITY_ENDERMAN_SCREAM, pos, volume, 0.45f);
            }
            // non-positional on purpose: both recordings are stereo — 3D audio would
            // refuse to pan them and could drop them entirely
            case VOICES -> client.getSoundManager().play(PositionedSoundInstance.master(
                    ru.exeswi.exest.registry.ModSounds.VOICESTART, 1.0f, volume));
            case APPROACH -> client.getSoundManager().play(PositionedSoundInstance.master(
                    ru.exeswi.exest.registry.ModSounds.FALL, 1.0f, volume));
        }
    }

    /** Distorted-music effect: kill the current track, put on something that is almost music. */
    public static void distortMusic(MinecraftClient client) {
        client.getSoundManager().stopSounds(null, SoundCategory.MUSIC);
        client.getMusicTracker().stop();
        client.getSoundManager().play(PositionedSoundInstance.master(
                SoundEvents.MUSIC_DISC_13.value(), 0.5f + RANDOM.nextFloat() * 0.2f, 0.35f));
    }

    /** Fires a cue locally without any server involvement — used by sanity hallucinations. */
    public static void localCue(MinecraftClient client, SoundCue cue, float volume) {
        playCue(client, SoundCuePayload.behindPlayer(cue, volume));
    }

    public static void tick(MinecraftClient client) {
        if (QUEUE.isEmpty() || client.world == null) {
            return;
        }
        Iterator<Pending> it = QUEUE.iterator();
        List<Pending> next = new ArrayList<>(QUEUE.size());
        while (it.hasNext()) {
            Pending pending = it.next();
            it.remove();
            if (pending.ticksLeft <= 0) {
                client.world.playSound(pending.pos.x, pending.pos.y, pending.pos.z,
                        pending.sound, SoundCategory.AMBIENT, pending.volume, pending.pitch, false);
            } else {
                next.add(new Pending(pending.ticksLeft - 1, pending.sound,
                        pending.pos, pending.volume, pending.pitch));
            }
        }
        QUEUE.addAll(next);
    }

    private static void schedule(int delay, SoundEvent sound, Vec3d pos, float volume, float pitch) {
        if (volume > 0.0f) {
            QUEUE.add(new Pending(delay, sound, pos, volume, pitch));
        }
    }

    private static Vec3d behindPlayer(MinecraftClient client) {
        Vec3d look = client.player.getRotationVec(1.0f);
        return client.player.getEyePos().subtract(look.multiply(2.5, 0.0, 2.5));
    }
}
