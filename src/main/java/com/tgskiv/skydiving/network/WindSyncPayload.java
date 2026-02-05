package com.tgskiv.skydiving.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record WindSyncPayload(Vec3d direction, double speed) implements CustomPayload {

    public static final Identifier ID = Identifier.of("paraglidingsimulator", "wind_sync");
    public static final CustomPayload.Id<WindSyncPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);

    // https://wiki.fabricmc.net/tutorial:codec
    public static final PacketCodec<PacketByteBuf, WindSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodec.of(
                    // encoder: writing to the packet
                    (value, buf) -> buf.writeDouble(value.x).writeDouble(value.y).writeDouble(value.z),
                    // decoder: reading the packet
                    buf -> new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
            ), WindSyncPayload::direction,

            PacketCodecs.DOUBLE, WindSyncPayload::speed,

            WindSyncPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
