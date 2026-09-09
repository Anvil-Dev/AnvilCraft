package dev.dubhe.anvilcraft.init.entity;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
        DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
        DeferredRegister.create(Registries.VILLAGER_PROFESSION, AnvilCraft.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> MONOLITH_CORE_POI = POI_TYPES.register(
        "monolith_core",
        () -> new PoiType(
            Stream.concat(
                ModBlocks.MONOLITH_CORE.get().getStateDefinition().getPossibleStates().stream(),
                ModBlocks.GIANT_MONOLITH_CORE.get().getStateDefinition().getPossibleStates().stream()
                    .filter(state -> state.getValue(GiantMonolithCoreBlock.HALF) == Cube3x3PartHalf.MID_CENTER)
            ).collect(Collectors.toSet()),
            0,
            1
        )
    );

    public static final DeferredHolder<PoiType, PoiType> JEWELER_POI = POI_TYPES.register(
        "jeweler_poi",
        () -> new PoiType(
            ImmutableSet.copyOf(ModBlocks.JEWEL_CRAFTING_TABLE
                .get()
                .getStateDefinition()
                .getPossibleStates()),
            1,
            1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> JEWELER = VILLAGER_PROFESSIONS.register(
        "jeweler",
        () -> new VillagerProfession(
            "jeweler",
            entry -> entry.value() == JEWELER_POI.get(),
            entry -> entry.value() == JEWELER_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_TOOLSMITH));

    public static final DeferredHolder<PoiType, PoiType> TRADING_STATION_POI = POI_TYPES.register(
        "trading_station_poi",
        () -> new PoiType(
            ImmutableSet.copyOf(ModBlocks.TRADING_STATION.get().getBottomStates()),
            1,
            1
        )
    );

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
