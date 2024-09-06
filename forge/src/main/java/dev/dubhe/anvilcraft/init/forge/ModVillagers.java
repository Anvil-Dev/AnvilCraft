package dev.dubhe.anvilcraft.init.forge;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.Lazy;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public class ModVillagers {
    public static final Lazy<PoiType> JEWELER_POI = new Lazy<>(
        () -> new PoiType(
            ImmutableSet.copyOf(ModBlocks.JEWEL_CRAFTING_TABLE.get().getStateDefinition().getPossibleStates()),
            1,
            1
        )
    );

    public static final Lazy<VillagerProfession> JEWELER = new Lazy<>(
        () -> new VillagerProfession(
            "jeweler",
            entry -> entry.get() == JEWELER_POI.get(),
            entry -> entry.get() == JEWELER_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_TOOLSMITH
        )
    );

    /**
     *
     */
    @SubscribeEvent
    public static void registerVillagers(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.POI_TYPES, helper -> {
            helper.register(AnvilCraft.of("jeweler_poi"), JEWELER_POI.get());
        });
        event.register(ForgeRegistries.Keys.VILLAGER_PROFESSIONS, helper -> {
            helper.register(AnvilCraft.of("jeweler"), JEWELER.get());
        });
    }
}
