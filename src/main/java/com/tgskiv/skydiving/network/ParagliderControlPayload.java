package com.tgskiv.skydiving.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S payload enviado cada tick cuando el jugador conduce el parapente.
 * turn: -1 izquierda, 0 sin giro, +1 derecha
 * forward/sideways: velocidades de entrada (W/S, A/D) normalizadas por el cliente
 */
public record ParagliderControlPayload(int turn, float forward, float sideways) implements CustomPayload {

    public static final CustomPayload.Id<ParagliderControlPayload> PAYLOAD_ID =
            new CustomPayload.Id<>(Identifier.of("paraglidingsimulator", "paraglider_control"));

    public static final PacketCodec<RegistryByteBuf, ParagliderControlPayload> CODEC =
            CustomPayload.codecOf(ParagliderControlPayload::write, ParagliderControlPayload::new);

    public ParagliderControlPayload(RegistryByteBuf buf) {
        this(buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.turn);
        buf.writeFloat(this.forward);
        buf.writeFloat(this.sideways);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
