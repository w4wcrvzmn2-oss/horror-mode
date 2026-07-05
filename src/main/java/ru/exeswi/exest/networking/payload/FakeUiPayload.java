package ru.exeswi.exest.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.networking.FakeUi;

/** S2C: show a fake crash/loading/title screen for a moment. */
public record FakeUiPayload(int typeId) implements CustomPayload {

    public static final CustomPayload.Id<FakeUiPayload> ID = new CustomPayload.Id<>(Exest.id("fake_ui"));

    public static final PacketCodec<RegistryByteBuf, FakeUiPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, FakeUiPayload::typeId,
            FakeUiPayload::new);

    public static FakeUiPayload of(FakeUi ui) {
        return new FakeUiPayload(ui.ordinal());
    }

    public FakeUi type() {
        return FakeUi.byId(typeId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
