package dev.dubhe.anvilcraft.init.entity;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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

    public static final ResourceKey<VillagerProfession> JEWELER_KEY = ResourceKey.create(
        Registries.VILLAGER_PROFESSION,
        AnvilCraft.of("jeweler")
    );

    public static final DeferredHolder<VillagerProfession, VillagerProfession> JEWELER = VILLAGER_PROFESSIONS.register(
        "jeweler",
        () -> new VillagerProfession(
            net.minecraft.network.chat.Component.translatable("entity.minecraft.villager.jeweler"),
            entry -> entry.is(JEWELER_POI.getKey()),
            entry -> entry.is(JEWELER_POI.getKey()),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_TOOLSMITH,
            Int2ObjectMap.ofEntries(
                Int2ObjectMap.entry(1, ModTradeSets.JEWELER_LEVEL_1),
                Int2ObjectMap.entry(2, ModTradeSets.JEWELER_LEVEL_2),
                Int2ObjectMap.entry(3, ModTradeSets.JEWELER_LEVEL_3),
                Int2ObjectMap.entry(4, ModTradeSets.JEWELER_LEVEL_4),
                Int2ObjectMap.entry(5, ModTradeSets.JEWELER_LEVEL_5)
            )
        ));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
