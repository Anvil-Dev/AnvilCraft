package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.number.INumberProvider;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.IResultModifier;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModRegistries {
    public static final ResourceKey<Registry<IAmulet.Type<?>>> AMULET_TYPE_KEY = ModRegistries.key("amulet_type");
    public static final Registry<IAmulet.Type<?>> AMULET_TYPE_REGISTRY = ModRegistries.simple(
        ModRegistries.AMULET_TYPE_KEY
    );

    public static final ResourceKey<Registry<IAmuletDefinition.Type<?>>> AMULET_DEF_TYPE_KEY = ModRegistries.key("amulet_definition_type");
    public static final Registry<IAmuletDefinition.Type<?>> AMULET_DEF_TYPE_REGISTRY = ModRegistries.simple(
        ModRegistries.AMULET_DEF_TYPE_KEY
    );

    public static final ResourceKey<Registry<IAmuletDefinition>> AMULET_DEF_KEY = ModRegistries.key("amulet_definition");

    public static final ResourceKey<Registry<BlockPlacementRuleSet>> BLOCK_PLACEMENT_RULES_KEY =
        ModRegistries.key("block_placement_rules");

    public static final ResourceKey<Registry<IResultModifier.Type<?>>> MODIFIER_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("result_modifier")
    );
    public static final Registry<IResultModifier.Type<?>> MODIFIER_TYPE_REGISTRY = new RegistryBuilder<>(MODIFIER_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<ICustomDataComponent.Type<?>>> CUSTOM_DATA_TYPE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("custom_data_component")
    );
    public static final Registry<ICustomDataComponent.Type<?>> CUSTOM_DATA_TYPE_REGISTRY = new RegistryBuilder<>(CUSTOM_DATA_TYPE_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<INumberProvider.Type<?>>> NUMBER_PROVIDER_TYPE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("number_provider")
    );
    public static final Registry<INumberProvider.Type<?>> NUMBER_PROVIDER_TYPE_REGISTRY = new RegistryBuilder<>(NUMBER_PROVIDER_TYPE_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<ITargetPointer.Type<?>>> TARGET_POINTER_TYPE_KEY = ModRegistries.key("target_pointer");
    public static final Registry<ITargetPointer.Type<?>> TARGET_POINTER_TYPE_REGISTRY = ModRegistries.simple(TARGET_POINTER_TYPE_KEY);

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(AMULET_TYPE_REGISTRY);
        event.register(AMULET_DEF_TYPE_REGISTRY);
        event.register(MODIFIER_TYPE_REGISTRY);
        event.register(CUSTOM_DATA_TYPE_REGISTRY);
        event.register(NUMBER_PROVIDER_TYPE_REGISTRY);
        event.register(TARGET_POINTER_TYPE_REGISTRY);
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ModRegistries.AMULET_DEF_KEY, IAmuletDefinition.DIRECT_CODEC, IAmuletDefinition.DIRECT_CODEC);
        event.dataPackRegistry(
            ModRegistries.BLOCK_PLACEMENT_RULES_KEY,
            BlockPlacementRuleSet.CODEC,
            BlockPlacementRuleSet.CODEC
        );
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(AnvilCraft.of(name));
    }

    private static <T> Registry<T> simple(ResourceKey<Registry<T>> key) {
        return new RegistryBuilder<>(key)
            .sync(true)
            .maxId(512)
            .create();
    }
}
