package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.network.AddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.AddTeslaFilterPacket;
import dev.dubhe.anvilcraft.network.ChargeCollectorIncomingChargePacket;
import dev.dubhe.anvilcraft.network.ChargerSyncPacket;
import dev.dubhe.anvilcraft.network.ComparatorSyncPacket;
import dev.dubhe.anvilcraft.network.CyclingValueSyncPacket;
import dev.dubhe.anvilcraft.network.DragonRodDevourPacket;
import dev.dubhe.anvilcraft.network.FilterContentSyncPacket;
import dev.dubhe.anvilcraft.network.HammerChangeBlockPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.network.HeatableSyncPacket;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPacket;
import dev.dubhe.anvilcraft.network.InspectionStateChangedPacket;
import dev.dubhe.anvilcraft.network.ItemDetectorChangeRangePacket;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.network.MachineCycleFilterModePacket;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPacket;
import dev.dubhe.anvilcraft.network.MultiphaseChangePacket;
import dev.dubhe.anvilcraft.network.MutedSoundSyncPacket;
import dev.dubhe.anvilcraft.network.PowerGridRemovePacket;
import dev.dubhe.anvilcraft.network.PowerGridSyncPacket;
import dev.dubhe.anvilcraft.network.PulseGeneratorUpdatePacket;
import dev.dubhe.anvilcraft.network.RecipeCacheSyncPacket;
import dev.dubhe.anvilcraft.network.RemoveMutedSoundPacket;
import dev.dubhe.anvilcraft.network.RemoveTeslaFilterPacket;
import dev.dubhe.anvilcraft.network.RocketJumpPacket;
import dev.dubhe.anvilcraft.network.SilencerSyncPacket;
import dev.dubhe.anvilcraft.network.SliderInitPacket;
import dev.dubhe.anvilcraft.network.SliderUpdatePacket;
import dev.dubhe.anvilcraft.network.SlidingEntitySyncPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.StructureDataSyncPacket;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import dev.dubhe.anvilcraft.network.SwitchPhasePacket;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
import dev.dubhe.anvilcraft.network.SyncEmberGrindstonePacket;
import dev.dubhe.anvilcraft.network.TeslaFilterSyncPacket;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {
    /**
     *
     */
    public static void init(PayloadRegistrar registrar) {
        registrar.playBidirectional(
            MachineOutputDirectionPacket.TYPE,
            MachineOutputDirectionPacket.STREAM_CODEC,
            MachineOutputDirectionPacket.HANDLER
        );
        registrar.playBidirectional(
            MachineEnableFilterPacket.TYPE,
            MachineEnableFilterPacket.STREAM_CODEC,
            MachineEnableFilterPacket.HANDLER
        );
        registrar.playBidirectional(
            SlotDisableChangePacket.TYPE,
            SlotDisableChangePacket.STREAM_CODEC,
            SlotDisableChangePacket.HANDLER
        );
        registrar.playBidirectional(
            SlotFilterChangePacket.TYPE,
            SlotFilterChangePacket.STREAM_CODEC,
            SlotFilterChangePacket.HANDLER
        );
        registrar.playToServer(
            SliderUpdatePacket.TYPE,
            SliderUpdatePacket.STREAM_CODEC,
            SliderUpdatePacket.HANDLER
        );
        registrar.playToClient(
            SliderInitPacket.TYPE,
            SliderInitPacket.STREAM_CODEC,
            SliderInitPacket.HANDLER);
        registrar.playToClient(
            PowerGridSyncPacket.TYPE,
            PowerGridSyncPacket.STREAM_CODEC,
            PowerGridSyncPacket.HANDLER);
        registrar.playToClient(
            PowerGridRemovePacket.TYPE,
            PowerGridRemovePacket.STREAM_CODEC,
            PowerGridRemovePacket.HANDLER
        );
        registrar.playToServer(
            HammerUsePacket.TYPE,
            HammerUsePacket.STREAM_CODEC,
            HammerUsePacket.HANDLER
        );
        registrar.playToServer(
            HammerChangeBlockPacket.TYPE,
            HammerChangeBlockPacket.STREAM_CODEC,
            HammerChangeBlockPacket::handle
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
            LaserEmitPacket.TYPE,
            LaserEmitPacket.STREAM_CODEC,
            LaserEmitPacket.HANDLER
        );
        registrar.playBidirectional(
            HeliostatsIrradiationPacket.TYPE,
            HeliostatsIrradiationPacket.STREAM_CODEC,
            HeliostatsIrradiationPacket.HANDLER
        );
        registrar.playToClient(
            UpdateDisplayItemPacket.TYPE,
            UpdateDisplayItemPacket.STREAM_CODEC,
            UpdateDisplayItemPacket.HANDLER
        );
        registrar.playToClient(
            StructureDataSyncPacket.TYPE,
            StructureDataSyncPacket.STREAM_CODEC,
            StructureDataSyncPacket.HANDLER
        );
        registrar.playToClient(
            ChargeCollectorIncomingChargePacket.TYPE,
            ChargeCollectorIncomingChargePacket.STREAM_CODEC,
            ChargeCollectorIncomingChargePacket::acceptClient
        );
        registrar.playToClient(
            InspectionStateChangedPacket.TYPE,
            InspectionStateChangedPacket.STREAM_CODEC,
            InspectionStateChangedPacket::acceptClient
        );
        registrar.playToClient(
            RecipeCacheSyncPacket.TYPE,
            RecipeCacheSyncPacket.STREAM_CODEC,
            RecipeCacheSyncPacket::acceptClient
        );
        registrar.playToClient(
            TeslaFilterSyncPacket.TYPE,
            TeslaFilterSyncPacket.STREAM_CODEC,
            TeslaFilterSyncPacket.HANDLER
        );
        registrar.playToServer(
            AddTeslaFilterPacket.TYPE,
            AddTeslaFilterPacket.STREAM_CODEC,
            AddTeslaFilterPacket.HANDLER
        );
        registrar.playToServer(
            RemoveTeslaFilterPacket.TYPE,
            RemoveTeslaFilterPacket.STREAM_CODEC,
            RemoveTeslaFilterPacket.HANDLER
        );
        registrar.playBidirectional(
            ItemDetectorChangeRangePacket.TYPE,
            ItemDetectorChangeRangePacket.STREAM_CODEC,
            ItemDetectorChangeRangePacket.HANDLER
        );
        registrar.playToServer(
            MachineCycleFilterModePacket.TYPE,
            MachineCycleFilterModePacket.STREAM_CODEC,
            MachineCycleFilterModePacket.HANDLER
        );
        registrar.playToServer(
            PulseGeneratorUpdatePacket.TYPE,
            PulseGeneratorUpdatePacket.STREAM_CODEC,
            PulseGeneratorUpdatePacket.HANDLER
        );
        registrar.playToServer(
            SwitchPhasePacket.TYPE,
            SwitchPhasePacket.STREAM_CODEC,
            SwitchPhasePacket.HANDLER
        );
        registrar.playBidirectional(
            MultiphaseChangePacket.TYPE,
            MultiphaseChangePacket.STREAM_CODEC,
            MultiphaseChangePacket.HANDLER
        );
        registrar.playToServer(
            SyncEmberGrindstonePacket.TYPE,
            SyncEmberGrindstonePacket.STREAM_CODEC,
            SyncEmberGrindstonePacket.HANDLER
        );
        registrar.playToServer(
            DragonRodDevourPacket.TYPE,
            DragonRodDevourPacket.STREAM_CODEC,
            DragonRodDevourPacket.HANDLER
        );
        registrar.playToServer(
            FilterContentSyncPacket.TYPE,
            FilterContentSyncPacket.STREAM_CODEC,
            FilterContentSyncPacket.HANDLER
        );
        registrar.playBidirectional(
            SilencerSyncPacket.TYPE,
            SilencerSyncPacket.STREAM_CODEC,
            SilencerSyncPacket.HANDLER
        );
        registrar.playToServer(
            SwitchResonateModePacket.TYPE,
            SwitchResonateModePacket.STREAM_CODEC,
            SwitchResonateModePacket.HANDLER
        );
        registrar.playToClient(
            HeatableSyncPacket.TYPE,
            HeatableSyncPacket.STREAM_CODEC,
            HeatableSyncPacket.HANDLER
        );
        registrar.playToClient(
            SlidingEntitySyncPacket.TYPE,
            SlidingEntitySyncPacket.STREAM_CODEC,
            SlidingEntitySyncPacket.HANDLER
        );
        registrar.playToClient(
            ChargerSyncPacket.TYPE,
            ChargerSyncPacket.STREAM_CODEC,
            ChargerSyncPacket.HANDLER
        );
        registrar.playToClient(
            ComparatorSyncPacket.TYPE,
            ComparatorSyncPacket.STREAM_CODEC,
            ComparatorSyncPacket.HANDLER
        );
        registrar.playToServer(
            SwitchMultitoolModePacket.TYPE,
            SwitchMultitoolModePacket.STREAM_CODEC,
            SwitchMultitoolModePacket.HANDLER
        );
    }
}
