package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.api.network.Networking;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.fabricmc.fabric.api.networking.v1.PacketType;

@SuppressWarnings("unused")
public class ModNetworks {
    public static final PacketType<MachineOutputDirectionPack> DIRECTION_PACKET_TYPE = Networking.CLIENT.register("machine_output_direction", MachineOutputDirectionPack.class, MachineOutputDirectionPack::new);
    public static final PacketType<MachineRecordMaterialPack> MATERIAL_PACKET_TYPE = Networking.CLIENT.register("machine_record_material", MachineRecordMaterialPack.class, MachineRecordMaterialPack::new);
    public static final PacketType<SlotDisableChangePack> SLOT_DISABLE_CHANGE_TYPE = Networking.CLIENT.register("slot_disable_change", SlotDisableChangePack.class, SlotDisableChangePack::new);
    public static final PacketType<SlotFilterChangePack> SLOT_FILTER_CHANGE_TYPE = Networking.CLIENT.register("slot_filter_change", SlotFilterChangePack.class, SlotFilterChangePack::new);

    public static void register() {
    }
}
