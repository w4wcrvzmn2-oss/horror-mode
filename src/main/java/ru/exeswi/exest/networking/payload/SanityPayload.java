package ru.exeswi.exest.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.exeswi.exest.Exest;

/** S2C: sync the player's hidden sanity and the world horror level for HUD/effects. */
public record SanityPayload(float sanity, int horrorLevel) implements CustomPayload {

    public static final CustomPayload.Id<SanityPayload> ID = new CustomPayload.Id<>(Exest.id("sanity"));

    public static final PacketCodec<RegistryByteBuf, SanityPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, SanityPayload::sanity,
            PacketCodecs.VAR_INT, SanityPayload::horrorLevel,
            SanityPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
