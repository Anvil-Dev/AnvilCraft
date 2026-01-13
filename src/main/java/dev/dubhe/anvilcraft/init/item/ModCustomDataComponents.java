package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.data.ItemEnchantmentsData;
import dev.dubhe.anvilcraft.api.recipe.data.MultiphaseData;
import dev.dubhe.anvilcraft.api.recipe.data.NormalDataComponent;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCustomDataComponents {
    private static final DeferredRegister<ICustomDataComponent.Type<?>> DF = DeferredRegister
        .create(ModRegistries.CUSTOM_DATA_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<ICustomDataComponent.Type<?>, NormalDataComponent.Type> NORMAL = DF
        .register("normal_data_component", NormalDataComponent.Type::new);

    public static final DeferredHolder<ICustomDataComponent.Type<?>, ItemEnchantmentsData.Type> ITEM_ENCHANTMENTS = DF
        .register("item_enchantments", ItemEnchantmentsData.Type::new);

    public static final DeferredHolder<ICustomDataComponent.Type<?>, MultiphaseData.Type> MULTIPHASE = DF
        .register("multiphase", MultiphaseData.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
