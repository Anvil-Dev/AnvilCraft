package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ApplyData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ChangeDataType;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.CopyData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.IResultModifier;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.MergeData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ModifyCount;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.RemoveAttribute;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.RemoveData;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModResultModifierTypes {
    private static final DeferredRegister<IResultModifier.Type<?>> DF = DeferredRegister
        .create(ModRegistries.MODIFIER_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IResultModifier.Type<?>, ApplyData.Type> APPLY_DATA = DF
        .register("apply_data", ApplyData.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, CopyData.Type> COPY_DATA = DF
        .register("copy_data", CopyData.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, MergeData.Type> MERGE_DATA = DF
        .register("merge_data", MergeData.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, RemoveData.Type> REMOVE_DATA = DF
        .register("remove_data", RemoveData.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, RemoveAttribute.Type> REMOVE_ATTRIBUTE = DF
        .register("remove_attribute", RemoveAttribute.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, ModifyCount.Type> MODIFY_COUNT = DF
        .register("modify_count", ModifyCount.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, ChangeDataType.Type> CHANGE_DATA_TYPE = DF
        .register("change_data_type", ChangeDataType.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
