package com.tgskiv.skydiving.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WindConfigSyncPayload(int ticksPerWindChange, double windRotationDegrees, double maxSpeedDelta, double maxWindSpeed, double minWindSpeed) implements CustomPayload {
    public static final Identifier DATA_ID = Identifier.of("paraglidingsimulator", "sync_config");
    public static final CustomPayload.Id<WindConfigSyncPayload> PAYLOAD_ID = new CustomPayload.Id<>(DATA_ID);


    public static final PacketCodec<PacketByteBuf, WindConfigSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, WindConfigSyncPayload::ticksPerWindChange,
                    PacketCodecs.DOUBLE, WindConfigSyncPayload::windRotationDegrees,
                    PacketCodecs.DOUBLE, WindConfigSyncPayload::maxSpeedDelta,
                    PacketCodecs.DOUBLE, WindConfigSyncPayload::maxWindSpeed,
                    PacketCodecs.DOUBLE, WindConfigSyncPayload::minWindSpeed,

                    WindConfigSyncPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
