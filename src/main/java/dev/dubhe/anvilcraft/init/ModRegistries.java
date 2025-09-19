package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.api.crate.category.ICategory;
import dev.dubhe.anvilcraft.api.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.IResultModifier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModRegistries {
    public static final ResourceKey<Registry<AmuletType>> AMULET_TYPE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("amulet_type")
    );
    public static final Registry<AmuletType> AMULET_TYPE_REGISTRY = new RegistryBuilder<>(AMULET_TYPE_KEY)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IResultModifier.Type<?>>> MODIFIER_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("result_modifier")
    );
    public static final Registry<IResultModifier.Type<?>> MODIFIER_TYPE_REGISTRY = new RegistryBuilder<>(MODIFIER_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<ICustomDataComponent.Type<?>>> CUSTOM_DATA_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("custom_data_component")
    );
    public static final Registry<ICustomDataComponent.Type<?>> CUSTOM_DATA_TYPE_REGISTRY = new RegistryBuilder<>(CUSTOM_DATA_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<ICategory>> CATEGORY_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("category")
    );
    public static final ResourceKey<Registry<ICategory.Type<?>>> CATEGORY_TYPE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("category_type")
    );
    public static final Registry<ICategory.Type<?>> CATEGORY_TYPE_REGISTRY = new RegistryBuilder<>(CATEGORY_TYPE_KEY)
        .sync(true)
        .maxId(512)
        .create();

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(AMULET_TYPE_REGISTRY);
        event.register(MODIFIER_TYPE_REGISTRY);
        event.register(CUSTOM_DATA_TYPE_REGISTRY);
        event.register(CATEGORY_TYPE_REGISTRY);
    }

    @SubscribeEvent
    public static void registerDataRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
            CATEGORY_KEY,
            ICategory.DIRECT_CODEC,
            ICategory.DIRECT_CODEC
        );
    }
}
