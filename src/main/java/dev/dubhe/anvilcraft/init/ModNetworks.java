package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.Network;
import dev.anvilcraft.lib.network.ClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.ClientRecipeManagerSyncPack;
import dev.dubhe.anvilcraft.ClientboundMutedSoundSyncPacket;
import dev.dubhe.anvilcraft.ClientboundUpdateDisplayItemPacket;
import dev.dubhe.anvilcraft.HammerUsePack;
import dev.dubhe.anvilcraft.HeliostatsIrradiationPack;
import dev.dubhe.anvilcraft.LaserEmitPack;
import dev.dubhe.anvilcraft.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.PowerGridRemovePack;
import dev.dubhe.anvilcraft.PowerGridSyncPack;
import dev.dubhe.anvilcraft.RocketJumpPacket;
import dev.dubhe.anvilcraft.ServerboundAddMutedSoundPacket;
import dev.dubhe.anvilcraft.ServerboundCyclingValueSyncPacket;
import dev.dubhe.anvilcraft.ServerboundRemoveMutedSoundPacket;
import dev.dubhe.anvilcraft.SliderInitPack;
import dev.dubhe.anvilcraft.SliderUpdatePack;
import dev.dubhe.anvilcraft.SlotDisableChangePack;
import dev.dubhe.anvilcraft.SlotFilterChangePack;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {
    public static final ResourceLocation DIRECTION_PACKET = register(
        "machine_output_direction",
        MachineOutputDirectionPack.class, MachineOutputDirectionPack::new
    );
    public static final ResourceLocation MATERIAL_PACKET = register(
        "machine_record_material",
        MachineEnableFilterPack.class, MachineEnableFilterPack::new
    );
    public static final ResourceLocation SLOT_DISABLE_CHANGE_PACKET = register(
        "slot_disable_change",
        SlotDisableChangePack.class, SlotDisableChangePack::new
    );
    public static final ResourceLocation SLOT_FILTER_CHANGE_PACKET = register(
        "slot_filter_change",
        SlotFilterChangePack.class, SlotFilterChangePack::new
    );
    public static final ResourceLocation SLIDER_UPDATE = register(
        "slider_update",
        SliderUpdatePack.class, SliderUpdatePack::new
    );
    public static final ResourceLocation SLIDER_INIT = register(
        "slider_init",
        SliderInitPack.class, SliderInitPack::new
    );
    public static final ResourceLocation POWER_GRID_SYNC = register(
        "power_grid_sync",
        PowerGridSyncPack.class, PowerGridSyncPack::new
    );
    public static final ResourceLocation POWER_GRID_REMOVE = register(
        "power_grid_remove",
        PowerGridRemovePack.class, PowerGridRemovePack::new
    );
    public static final ResourceLocation HAMMER_USE = register(
        "hammer_use",
        HammerUsePack.class, HammerUsePack::new
    );
    public static final ResourceLocation CYCLING_VALUE = register(
        "cycling_value",
        ServerboundCyclingValueSyncPacket.class, ServerboundCyclingValueSyncPacket::new
    );
    public static final ResourceLocation ROCKET_JUMP = register(
        "rocket_jump",
        RocketJumpPacket.class, RocketJumpPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_SYNC = register(
            "muted_sound_sync",
            ClientboundMutedSoundSyncPacket.class, ClientboundMutedSoundSyncPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_ADD = register(
            "muted_sound_add",
            ServerboundAddMutedSoundPacket.class, ServerboundAddMutedSoundPacket::new
    );

    public static final ResourceLocation MUTED_SOUND_REMOVE = register(
        "muted_sound_remove",
        ServerboundRemoveMutedSoundPacket.class, ServerboundRemoveMutedSoundPacket::new
    );

    public static final ResourceLocation LASER_EMIT = register(
        "laser_emit",
        LaserEmitPack.class, LaserEmitPack::new
    );

    public static final ResourceLocation HELIOSTATS_IRRADIATION = register(
        "heliostats_irradiation_pack",
        HeliostatsIrradiationPack.class, HeliostatsIrradiationPack::new
    );

    public static final ResourceLocation CLIENT_RECIPE_MANAGER_SYNC = register(
        "client_recipe_manager_sync",
        ClientRecipeManagerSyncPack.class, ClientRecipeManagerSyncPack::new
    );

    public static final ResourceLocation CLIENT_UPDATE_DISPLAY_ITEM = register(
            "client_update_display_item",
            ClientboundUpdateDisplayItemPacket.class, ClientboundUpdateDisplayItemPacket::new
    );

    public static void register(PayloadRegistrar registrar) {

    }
}
