package dev.dubhe.anvilcraft.init.item;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItemSubPredicates {
    private static final DeferredRegister<ItemSubPredicate.Type<?>> DF = DeferredRegister.create(
        BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<ItemSavedEntityPredicate>> SAVED_ENTITY = register(
        "saved_entity",
        ItemSavedEntityPredicate.CODEC
    );

    public static <T extends ItemSubPredicate> DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<T>> register(
        String name,
        Codec<T> codec
    ) {
        return DF.register(name, () -> new ItemSubPredicate.Type<>(codec));
    }

    public static void initialize(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
