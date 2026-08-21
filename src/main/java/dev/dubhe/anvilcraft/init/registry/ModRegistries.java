package dev.dubhe.anvilcraft.init.registry;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.number.INumberProvider;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.IResultModifier;
import dev.dubhe.anvilcraft.block.entity.celestial.Megastructure;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModRegistries {
    public static final Registry<IAmulet.Type<?>> AMULET_TYPE = ModRegistries.simple(
        ModRegistryKeys.AMULET_TYPE
    );
    public static final Registry<IAmuletDefinition.Type<?>> AMULET_DEF_TYPE = ModRegistries.simple(
        ModRegistryKeys.AMULET_DEF_TYPE
    );
    public static final Registry<IResultModifier.Type<?>> MODIFIER_TYPE = ModRegistries.simple(
        ModRegistryKeys.MODIFIER
    );
    public static final Registry<ICustomDataComponent.Type<?>> CUSTOM_DATA_TYPE = ModRegistries.simple(
        ModRegistryKeys.CUSTOM_DATA_TYPE
    );
    public static final Registry<INumberProvider.Type<?>> NUMBER_PROVIDER_TYPE = ModRegistries.simple(
        ModRegistryKeys.NUMBER_PROVIDER_TYPE
    );
    public static final Registry<ICategory.Type<?>> CATEGORY_TYPE = ModRegistries.simple(
        ModRegistryKeys.CATEGORY_TYPE
    );
    public static final Registry<ITargetPointer.Type<?>> TARGET_POINTER_TYPE = ModRegistries.simple(
        ModRegistryKeys.TARGET_POINTER_TYPE
    );
    /** Code-registered CFA megastructure definitions. */
    public static final Registry<Megastructure> MEGASTRUCTURE = ModRegistries.simple(
        ModRegistryKeys.MEGASTRUCTURE
    );

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(ModRegistries.AMULET_TYPE);
        event.register(ModRegistries.AMULET_DEF_TYPE);
        event.register(ModRegistries.MODIFIER_TYPE);
        event.register(ModRegistries.CUSTOM_DATA_TYPE);
        event.register(ModRegistries.NUMBER_PROVIDER_TYPE);
        event.register(ModRegistries.CATEGORY_TYPE);
        event.register(ModRegistries.TARGET_POINTER_TYPE);
        event.register(ModRegistries.MEGASTRUCTURE);
    }

    private static <T> Registry<T> simple(ResourceKey<Registry<T>> key) {
        return new RegistryBuilder<>(key)
            .sync(true)
            .maxId(512)
            .create();
    }
}
