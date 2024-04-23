package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.network.HammerUsePack;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import dev.dubhe.anvilcraft.network.PowerGridSyncPack;
import dev.dubhe.anvilcraft.network.SliderInitPack;
import dev.dubhe.anvilcraft.network.SliderUpdatePack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.resources.ResourceLocation;

public class ModNetworks {
    public static final ResourceLocation DIRECTION_PACKET = Network.register(
        AnvilCraft.of("machine_output_direction"),
        MachineOutputDirectionPack.class, MachineOutputDirectionPack::new
    );
    public static final ResourceLocation MATERIAL_PACKET = Network.register(
        AnvilCraft.of("machine_record_material"),
        MachineEnableFilterPack.class, MachineEnableFilterPack::new
    );
    public static final ResourceLocation SLOT_DISABLE_CHANGE_PACKET = Network.register(
        AnvilCraft.of("slot_disable_change"),
        SlotDisableChangePack.class, SlotDisableChangePack::new
    );
    public static final ResourceLocation SLOT_FILTER_CHANGE_PACKET = Network.register(
        AnvilCraft.of("slot_filter_change"),
        SlotFilterChangePack.class, SlotFilterChangePack::new
    );
    public static final ResourceLocation SLIDER_UPDATE = Network.register(
        AnvilCraft.of("slider_update"),
        SliderUpdatePack.class, SliderUpdatePack::new
    );
    public static final ResourceLocation SLIDER_INIT = Network.register(
        AnvilCraft.of("slider_init"),
        SliderInitPack.class, SliderInitPack::new
    );
    public static final ResourceLocation POWER_GRID_SYNC = Network.register(
        AnvilCraft.of("power_grid_sync"),
        PowerGridSyncPack.class, PowerGridSyncPack::new
    );
    public static final ResourceLocation POWER_GRID_REMOVE = Network.register(
        AnvilCraft.of("power_grid_remove"),
        PowerGridRemovePack.class, PowerGridRemovePack::new
    );
    public static final ResourceLocation HAMMER_USE = Network.register(
        AnvilCraft.of("hammer_use"),
        HammerUsePack.class, HammerUsePack::new
    );

    public static void register() {
    }
}
