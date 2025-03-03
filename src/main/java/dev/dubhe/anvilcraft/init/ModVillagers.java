package dev.dubhe.anvilcraft.init;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
        DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
        DeferredRegister.create(Registries.VILLAGER_PROFESSION, AnvilCraft.MOD_ID);

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

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
