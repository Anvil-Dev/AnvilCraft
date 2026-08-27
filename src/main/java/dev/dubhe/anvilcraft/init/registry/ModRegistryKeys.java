package dev.dubhe.anvilcraft.init.registry;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.number.INumberProvider;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.IResultModifier;
import dev.dubhe.anvilcraft.block.entity.celestial.Megastructure;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModRegistryKeys {
    public static final ResourceKey<Registry<IAmulet.Type<?>>> AMULET_TYPE = ModRegistryKeys.key("amulet_type");
    public static final ResourceKey<Registry<IAmuletDefinition.Type<?>>> AMULET_DEF_TYPE = ModRegistryKeys.key("amulet_definition_type");
    public static final ResourceKey<Registry<IAmuletDefinition>> AMULET_DEF = ModRegistryKeys.key("amulet_definition");
    public static final ResourceKey<Registry<IResultModifier.Type<?>>> MODIFIER = ModRegistryKeys.key("result_modifier");
    public static final ResourceKey<Registry<ICustomDataComponent.Type<?>>> CUSTOM_DATA_TYPE = ModRegistryKeys.key("custom_data_component");
    public static final ResourceKey<Registry<INumberProvider.Type<?>>> NUMBER_PROVIDER_TYPE = ModRegistryKeys.key("number_provider");
    public static final ResourceKey<Registry<ICategory.Type<?>>> CATEGORY_TYPE = ModRegistryKeys.key("category_type");
    public static final ResourceKey<Registry<ICategory>> CATEGORY = ModRegistryKeys.key("category");
    public static final ResourceKey<Registry<ITargetPointer.Type<?>>> TARGET_POINTER_TYPE = ModRegistryKeys.key("target_pointer");
    public static final ResourceKey<Registry<BlockPlacementRuleSet>> BLOCK_PLACEMENT_RULES = ModRegistryKeys.key("block_placement_rules");
    public static final ResourceKey<Registry<Megastructure>> MEGASTRUCTURE = ModRegistryKeys.key("megastructure");
    public static final ResourceKey<Registry<IStorageType<?>>> STORAGE_TYPE = ModRegistryKeys.key("storage_type");

    @SubscribeEvent
    public static void registerRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ModRegistryKeys.AMULET_DEF, IAmuletDefinition.DIRECT_CODEC, IAmuletDefinition.DIRECT_CODEC);
        event.dataPackRegistry(ModRegistryKeys.CATEGORY, ICategory.DIRECT_CODEC, ICategory.DIRECT_CODEC);
        event.dataPackRegistry(
            ModRegistryKeys.BLOCK_PLACEMENT_RULES,
            BlockPlacementRuleSet.CODEC,
            BlockPlacementRuleSet.CODEC
        );
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(AnvilCraft.of(name));
    }
}
