package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.network.Network;
import dev.dubhe.anvilcraft.network.ClientboundMutedSoundSyncPacket;
import dev.dubhe.anvilcraft.network.HammerUsePack;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPack;
import dev.dubhe.anvilcraft.network.LaserEmitPack;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import dev.dubhe.anvilcraft.network.PowerGridSyncPack;
import dev.dubhe.anvilcraft.network.RocketJumpPacket;
import dev.dubhe.anvilcraft.network.ServerboundAddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.ServerboundCyclingValueSyncPacket;
import dev.dubhe.anvilcraft.network.ServerboundRemoveMutedSoundPacket;
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
    public static final ResourceLocation CYCLING_VALUE = Network.register(
        AnvilCraft.of("cycling_value"),
        ServerboundCyclingValueSyncPacket.class, ServerboundCyclingValueSyncPacket::new
    );
    public static final ResourceLocation ROCKET_JUMP = Network.register(
        AnvilCraft.of("rocket_jump"),
        RocketJumpPacket.class, RocketJumpPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_SYNC = Network.register(
            AnvilCraft.of("muted_sound_sync"),
            ClientboundMutedSoundSyncPacket.class, ClientboundMutedSoundSyncPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_ADD = Network.register(
            AnvilCraft.of("muted_sound_add"),
            ServerboundAddMutedSoundPacket.class, ServerboundAddMutedSoundPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_REMOVE = Network.register(
        AnvilCraft.of("muted_sound_remove"),
        ServerboundRemoveMutedSoundPacket.class, ServerboundRemoveMutedSoundPacket::new
    );

    public static final ResourceLocation LASER_EMIT = Network.register(
        AnvilCraft.of("laser_emit"),
        LaserEmitPack.class, LaserEmitPack::new
    );

    public static final ResourceLocation HELIOSTATS_IRRADIATION = Network.register(
        AnvilCraft.of("heliostats_irradiation_pack"),
        HeliostatsIrradiationPack.class, HeliostatsIrradiationPack::new
    );

    public static void register() {
    }
}
