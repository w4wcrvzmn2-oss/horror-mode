package ru.exeswi.exest.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.exeswi.exest.Exest;

/**
 * S2C: ambient "mood" push. Darkness and fog are target intensities the client eases
 * towards; flags toggle sustained states for {@code durationTicks}.
 */
public record MoodPayload(float darkness, float fog, int flags, int durationTicks) implements CustomPayload {

    public static final int FLAG_RED_MOON = 1;
    public static final int FLAG_SILENCE = 1 << 1;
    public static final int FLAG_COMPASS_SPIN = 1 << 2;
    public static final int FLAG_LIGHT_FLICKER = 1 << 3;

    public static final CustomPayload.Id<MoodPayload> ID = new CustomPayload.Id<>(Exest.id("mood"));

    public static final PacketCodec<RegistryByteBuf, MoodPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, MoodPayload::darkness,
            PacketCodecs.FLOAT, MoodPayload::fog,
            PacketCodecs.VAR_INT, MoodPayload::flags,
            PacketCodecs.VAR_INT, MoodPayload::durationTicks,
            MoodPayload::new);

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
