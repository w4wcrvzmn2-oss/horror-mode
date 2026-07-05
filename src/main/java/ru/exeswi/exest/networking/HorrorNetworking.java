package ru.exeswi.exest.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.networking.payload.EffectPayload;
import ru.exeswi.exest.networking.payload.FakeUiPayload;
import ru.exeswi.exest.networking.payload.MoodPayload;
import ru.exeswi.exest.networking.payload.SanityPayload;
import ru.exeswi.exest.networking.payload.SoundCuePayload;

/** Registers all payload types and offers small send helpers for the server side. */
public final class HorrorNetworking {

    private HorrorNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(EffectPayload.ID, EffectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SoundCuePayload.ID, SoundCuePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MoodPayload.ID, MoodPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FakeUiPayload.ID, FakeUiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SanityPayload.ID, SanityPayload.CODEC);
    }

    public static void sendEffect(ServerPlayerEntity player, HorrorEffect effect, float intensity, int duration) {
        ServerPlayNetworking.send(player, EffectPayload.of(effect, intensity, duration));
    }

    public static void sendCueAt(ServerPlayerEntity player, SoundCue cue, Vec3d pos, float volume) {
        ServerPlayNetworking.send(player, SoundCuePayload.at(cue, pos, volume));
    }

    public static void sendCueBehind(ServerPlayerEntity player, SoundCue cue, float volume) {
        ServerPlayNetworking.send(player, SoundCuePayload.behindPlayer(cue, volume));
    }

    public static void sendMood(ServerPlayerEntity player, float darkness, float fog, int flags, int duration) {
        ServerPlayNetworking.send(player, new MoodPayload(darkness, fog, flags, duration));
    }

    public static void sendFakeUi(ServerPlayerEntity player, FakeUi ui) {
        ServerPlayNetworking.send(player, FakeUiPayload.of(ui));
    }

    public static void sendSanity(ServerPlayerEntity player, float sanity, int horrorLevel) {
        ServerPlayNetworking.send(player, new SanityPayload(sanity, horrorLevel));
    }
}
