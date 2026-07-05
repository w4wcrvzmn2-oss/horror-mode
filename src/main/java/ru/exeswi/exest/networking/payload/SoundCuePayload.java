package ru.exeswi.exest.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.networking.SoundCue;

/**
 * S2C: play a positional horror sound cue. When {@code relative} is true, x/y/z are an
 * offset from the player's ears (used for "right behind you" sounds); otherwise they
 * are absolute world coordinates.
 */
public record SoundCuePayload(int cueId, boolean relative, double x, double y, double z,
                              float volume) implements CustomPayload {

    public static final CustomPayload.Id<SoundCuePayload> ID = new CustomPayload.Id<>(Exest.id("sound_cue"));

    public static final PacketCodec<RegistryByteBuf, SoundCuePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SoundCuePayload::cueId,
            PacketCodecs.BOOL, SoundCuePayload::relative,
            PacketCodecs.DOUBLE, SoundCuePayload::x,
            PacketCodecs.DOUBLE, SoundCuePayload::y,
            PacketCodecs.DOUBLE, SoundCuePayload::z,
            PacketCodecs.FLOAT, SoundCuePayload::volume,
            SoundCuePayload::new);

    public static SoundCuePayload at(SoundCue cue, Vec3d pos, float volume) {
        return new SoundCuePayload(cue.ordinal(), false, pos.x, pos.y, pos.z, volume);
    }

    public static SoundCuePayload behindPlayer(SoundCue cue, float volume) {
        return new SoundCuePayload(cue.ordinal(), true, 0, 0, 0, volume);
    }

    public SoundCue cue() {
        return SoundCue.byId(cueId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
