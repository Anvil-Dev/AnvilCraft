package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.api.network.Networking;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import net.fabricmc.fabric.api.networking.v1.PacketType;

public class ModNetworks {
    public static final PacketType<MachineOutputDirectionPack> DIRECTION_PACKET_TYPE = Networking.CLIENT.register("machine_output_direction", MachineOutputDirectionPack.class, MachineOutputDirectionPack::new);
    public static final PacketType<MachineRecordMaterialPack> MATERIAL_PACKET_TYPE = Networking.CLIENT.register("machine_record_material", MachineRecordMaterialPack.class, MachineRecordMaterialPack::new);
    public static void register(){}
}
