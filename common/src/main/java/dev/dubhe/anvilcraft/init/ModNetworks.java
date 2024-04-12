package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.resources.ResourceLocation;

public class ModNetworks {
    public static final ResourceLocation DIRECTION_PACKET = Network
        .register(AnvilCraft.of("machine_output_direction"),
            MachineOutputDirectionPack.class, MachineOutputDirectionPack::new);
    public static final ResourceLocation MATERIAL_PACKET = Network
        .register(AnvilCraft.of("machine_record_material"),
            MachineEnableFilterPack.class, MachineEnableFilterPack::new);
    public static final ResourceLocation SLOT_DISABLE_CHANGE_PACKET = Network
        .register(AnvilCraft.of("slot_disable_change"),
            SlotDisableChangePack.class, SlotDisableChangePack::new);
    public static final ResourceLocation SLOT_FILTER_CHANGE_PACKET = Network
        .register(AnvilCraft.of("slot_filter_change"),
            SlotFilterChangePack.class, SlotFilterChangePack::new);

    public static void register() {
    }
}
