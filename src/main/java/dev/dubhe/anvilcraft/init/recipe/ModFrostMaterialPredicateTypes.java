package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.recipe.frost.AndFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.CustomFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.EmptyFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.IFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.OrFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.RepairMaterialFrostMaterialPredicate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFrostMaterialPredicateTypes {
    private static final DeferredRegister<IFrostMaterialPredicate.Type<?>> DF = DeferredRegister
        .create(ModRegistries.FROST_MATERIAL_PREDICATE_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IFrostMaterialPredicate.Type<?>, AndFrostMaterialPredicate.Type> AND = DF
        .register("and", AndFrostMaterialPredicate.Type::new);

    public static final DeferredHolder<IFrostMaterialPredicate.Type<?>, OrFrostMaterialPredicate.Type> OR = DF
        .register("or", OrFrostMaterialPredicate.Type::new);

    public static final DeferredHolder<IFrostMaterialPredicate.Type<?>, RepairMaterialFrostMaterialPredicate.Type>
        REPAIR_MATERIAL = DF.register("repair_material", RepairMaterialFrostMaterialPredicate.Type::new);

    public static final DeferredHolder<IFrostMaterialPredicate.Type<?>, CustomFrostMaterialPredicate.Type> CUSTOM = DF
        .register("custom", CustomFrostMaterialPredicate.Type::new);

    public static final DeferredHolder<IFrostMaterialPredicate.Type<?>, EmptyFrostMaterialPredicate.Type> EMPTY = DF
        .register("empty", EmptyFrostMaterialPredicate.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
