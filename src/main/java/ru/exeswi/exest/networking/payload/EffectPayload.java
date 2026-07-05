package ru.exeswi.exest.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.networking.HorrorEffect;

/** S2C: fire a one-shot visual/hallucination effect on the client. */
public record EffectPayload(int effectId, float intensity, int durationTicks) implements CustomPayload {

    public static final CustomPayload.Id<EffectPayload> ID = new CustomPayload.Id<>(Exest.id("effect"));

    public static final PacketCodec<RegistryByteBuf, EffectPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, EffectPayload::effectId,
            PacketCodecs.FLOAT, EffectPayload::intensity,
            PacketCodecs.VAR_INT, EffectPayload::durationTicks,
            EffectPayload::new);

    public static EffectPayload of(HorrorEffect effect, float intensity, int durationTicks) {
        return new EffectPayload(effect.ordinal(), intensity, durationTicks);
    }

    public HorrorEffect effect() {
        return HorrorEffect.byId(effectId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
