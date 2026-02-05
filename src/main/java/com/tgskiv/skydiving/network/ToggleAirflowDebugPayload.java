package com.tgskiv.skydiving.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleAirflowDebugPayload(boolean visible) implements CustomPayload {

    public static final Identifier ID = Identifier.of("paraglidingsimulator", "toggle_airflow_debug");
    public static final CustomPayload.Id<ToggleAirflowDebugPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);


    public static final PacketCodec<PacketByteBuf, ToggleAirflowDebugPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ToggleAirflowDebugPayload::visible,
            ToggleAirflowDebugPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
