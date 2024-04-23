package dev.dubhe.anvilcraft.init.fabric;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;

public class ModVillagers {
    public static final ResourceKey<PoiType> JEWELER_POI_KEY = poiKey("jeweler_poi");
    public static final PoiType JEWELER_POI = registerPoi("jeweler_poi", ModBlocks.JEWEL_CRAFTING_TABLE.get());
    public static final VillagerProfession JEWELER = registerProfession("jeweler", JEWELER_POI_KEY);

    private static VillagerProfession registerProfession(String name, ResourceKey<PoiType> type) {
        return Registry.register(
            BuiltInRegistries.VILLAGER_PROFESSION,
            AnvilCraft.of(name),
            new VillagerProfession(
                name, entry -> entry.is(type), entry -> entry.is(type),
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH
            ));
    }

    private static ResourceKey<PoiType> poiKey(String name) {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, AnvilCraft.of(name));
    }

    private static PoiType registerPoi(String name, Block block) {
        return PointOfInterestHelper.register(AnvilCraft.of(name), 1, 1, block);
    }

    public static void register() {

    }
}
