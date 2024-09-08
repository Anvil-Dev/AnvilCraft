package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.network.AddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.CyclingValueSyncPacket;
import dev.dubhe.anvilcraft.network.HammerUsePack;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPack;
import dev.dubhe.anvilcraft.network.LaserEmitPack;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MutedSoundSyncPacket;
import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import dev.dubhe.anvilcraft.network.PowerGridSyncPack;
import dev.dubhe.anvilcraft.network.RemoveMutedSoundPacket;
import dev.dubhe.anvilcraft.network.RocketJumpPacket;
import dev.dubhe.anvilcraft.network.SliderInitPack;
import dev.dubhe.anvilcraft.network.SliderUpdatePack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {
    /**
     *
     */
    public static void init(PayloadRegistrar registrar) {
        registrar.playBidirectional(
            MachineOutputDirectionPack.TYPE,
            MachineOutputDirectionPack.STREAM_CODEC,
            MachineOutputDirectionPack.HANDLER
        );
        registrar.playBidirectional(
            MachineEnableFilterPack.TYPE,
            MachineEnableFilterPack.STREAM_CODEC,
            MachineEnableFilterPack.HANDLER
        );
        registrar.playBidirectional(
            SlotDisableChangePack.TYPE,
            SlotDisableChangePack.STREAM_CODEC,
            SlotDisableChangePack.HANDLER
        );
        registrar.playBidirectional(
            SlotFilterChangePack.TYPE,
            SlotFilterChangePack.STREAM_CODEC,
            SlotFilterChangePack.HANDLER
        );
        registrar.playToServer(
            SliderUpdatePack.TYPE,
            SliderUpdatePack.STREAM_CODEC,
            SliderUpdatePack.HANDLER
        );
        registrar.playToClient(
            SliderInitPack.TYPE,
            SliderInitPack.STREAM_CODEC,
            SliderInitPack.HANDLER
        );
        registrar.playToClient(
            PowerGridSyncPack.TYPE,
            PowerGridSyncPack.STREAM_CODEC,
            PowerGridSyncPack.HANDLER
        );
        registrar.playToClient(
            PowerGridRemovePack.TYPE,
            PowerGridRemovePack.STREAM_CODEC,
            PowerGridRemovePack.HANDLER
        );
        registrar.playToServer(
            HammerUsePack.TYPE,
            HammerUsePack.STREAM_CODEC,
            HammerUsePack.HANDLER
        );
        registrar.playToServer(
            CyclingValueSyncPacket.TYPE,
            CyclingValueSyncPacket.STREAM_CODEC,
            CyclingValueSyncPacket.HANDLER
        );
        registrar.playToClient(
            RocketJumpPacket.TYPE,
            RocketJumpPacket.STREAM_CODEC,
            RocketJumpPacket.HANDLER
        );
        registrar.playToClient(
            MutedSoundSyncPacket.TYPE,
            MutedSoundSyncPacket.STREAM_CODEC,
            MutedSoundSyncPacket.HANDLER
        );
        registrar.playToServer(
            AddMutedSoundPacket.TYPE,
            AddMutedSoundPacket.STREAM_CODEC,
            AddMutedSoundPacket.HANDLER
        );
        registrar.playToServer(
            RemoveMutedSoundPacket.TYPE,
            RemoveMutedSoundPacket.STREAM_CODEC,
            RemoveMutedSoundPacket.HANDLER
        );
        registrar.playToClient(
            LaserEmitPack.TYPE,
            LaserEmitPack.STREAM_CODEC,
            LaserEmitPack.HANDLER
        );
        registrar.playBidirectional(
            HeliostatsIrradiationPack.TYPE,
            HeliostatsIrradiationPack.STREAM_CODEC,
            HeliostatsIrradiationPack.HANDLER
        );
        registrar.playToClient(
            UpdateDisplayItemPacket.TYPE,
            UpdateDisplayItemPacket.STREAM_CODEC,
            UpdateDisplayItemPacket.HANDLER
        );
    }
}
