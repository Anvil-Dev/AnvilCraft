package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.api.network.Networking;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotChangePack;
import net.fabricmc.fabric.api.networking.v1.PacketType;

public class ModNetworks {
    public static final PacketType<MachineOutputDirectionPack> DIRECTION_PACKET_TYPE = Networking.SERVER.register("machine_output_direction", MachineOutputDirectionPack.class, MachineOutputDirectionPack::new);
    public static final PacketType<MachineRecordMaterialPack> MATERIAL_PACKET_TYPE = Networking.SERVER.register("machine_record_material", MachineRecordMaterialPack.class, MachineRecordMaterialPack::new);
    public static final PacketType<SlotChangePack> SLOT_CHANGE_TYPE = Networking.SERVER.register("slot_change", SlotChangePack.class, SlotChangePack::new);

    public static void register() {
    }
}
