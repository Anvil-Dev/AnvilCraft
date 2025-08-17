package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.api.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.IResultModifier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModRegistries {
    public static final ResourceKey<Registry<AmuletType>> AMULET_TYPE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("amulet_type")
    );
    public static final Registry<AmuletType> AMULET_TYPE_REGISTRY = new RegistryBuilder<>(AMULET_TYPE_KEY)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IRecipeTrigger>> TRIGGER_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("trigger")
    );
    public static final Registry<IRecipeTrigger> TRIGGER_REGISTRY = new RegistryBuilder<>(TRIGGER_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IRecipeOutcome.Type<?>>> OUTCOME_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("outcome")
    );
    public static final Registry<IRecipeOutcome.Type<?>> OUTCOME_TYPE_REGISTRY = new RegistryBuilder<>(OUTCOME_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IRecipePredicate.Type<?>>> PREDICATE_KEY = ResourceKey.createRegistryKey(
        AnvilCraft.of("predicate")
    );
    public static final Registry<IRecipePredicate.Type<?>> PREDICATE_TYPE_REGISTRY = new RegistryBuilder<>(PREDICATE_KEY)
        .sync(true)
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

    @SubscribeEvent
    public static void registerRegistries(@NotNull NewRegistryEvent event) {
        event.register(AMULET_TYPE_REGISTRY);
        event.register(TRIGGER_REGISTRY);
        event.register(OUTCOME_TYPE_REGISTRY);
        event.register(PREDICATE_TYPE_REGISTRY);
        event.register(MODIFIER_TYPE_REGISTRY);
        event.register(CUSTOM_DATA_TYPE_REGISTRY);
    }
}
