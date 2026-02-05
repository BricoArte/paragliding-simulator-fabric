package com.tgskiv.skydiving.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ThermalSyncPayload(List<ThermalSnapshot> thermals) implements CustomPayload {

    public static final Identifier ID = Identifier.of("paraglidingsimulator", "thermal_sync");
    public static final CustomPayload.Id<ThermalSyncPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, ThermalSyncPayload> CODEC =
            CustomPayload.codecOf(ThermalSyncPayload::write, ThermalSyncPayload::new);

    public ThermalSyncPayload(RegistryByteBuf buf) {
        this(readSnapshots(buf));
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(thermals.size());
        for (ThermalSnapshot thermal : thermals) {
            buf.writeDouble(thermal.centerX);
            buf.writeDouble(thermal.centerZ);
            buf.writeDouble(thermal.cloudY);
            buf.writeDouble(thermal.factor);
            buf.writeDouble(thermal.sizeFactor);
        }
    }

    private static List<ThermalSnapshot> readSnapshots(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<ThermalSnapshot> snapshots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            snapshots.add(new ThermalSnapshot(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
        return snapshots;
    }

    public record ThermalSnapshot(double centerX, double centerZ, double cloudY, double factor, double sizeFactor) {}

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
