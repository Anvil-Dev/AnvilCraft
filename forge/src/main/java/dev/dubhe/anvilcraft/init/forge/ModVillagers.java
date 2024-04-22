package dev.dubhe.anvilcraft.init.forge;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(
        ForgeRegistries.POI_TYPES, AnvilCraft.MOD_ID
    );

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(
        ForgeRegistries.VILLAGER_PROFESSIONS, AnvilCraft.MOD_ID
    );

    public static final RegistryObject<PoiType> JEWELER_POI = POI_TYPES.register(
        "jeweler_poi", () -> new PoiType(
            ImmutableSet.copyOf(ModBlocks.JEWEL_CRAFTING_TABLE.get().getStateDefinition().getPossibleStates()),
            1, 1
        )
    );

    public static final RegistryObject<VillagerProfession> JEWELER = VILLAGER_PROFESSIONS.register(
        "jeweler", () -> new VillagerProfession(
            "jeweler", entry -> entry.get() == JEWELER_POI.get(), entry -> entry.get() == JEWELER_POI.get(),
            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH
        )
    );

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
