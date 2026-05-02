package dev.dubhe.anvilcraft.init.item;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.predicate.ItemEnchantmentCountPredicate;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponentPredicates {
    private static final DeferredRegister<DataComponentPredicate.Type<?>> DF = DeferredRegister.create(
        BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<ItemSavedEntityPredicate>> SAVED_ENTITY = register(
        "saved_entity",
        ItemSavedEntityPredicate.CODEC
    );

    public static final DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<ItemEnchantmentCountPredicate>> ENCHANTMENT_COUNT =
        register(
            "enchantment_count",
            ItemEnchantmentCountPredicate.CODEC.codec()
        );

    public static <T extends DataComponentPredicate> DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<T>> register(
        String name,
        Codec<T> codec
    ) {
        return DF.register(name, () -> new DataComponentPredicate.Type<>(codec));
    }

    public static void initialize(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
