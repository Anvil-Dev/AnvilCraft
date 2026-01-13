package dev.dubhe.anvilcraft.init.recipe;

import dev.anvilcraft.lib.recipe.init.LibRegistries;
import dev.anvilcraft.lib.recipe.predicate.IRecipePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipePredicateTypes {
    public static final DeferredRegister<IRecipePredicate.Type<?>> PREDICATE_TYPE = DeferredRegister
        .create(LibRegistries.PREDICATE_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IRecipePredicate.Type<?>, HasCauldron.Type> HAS_CAULDRON = PREDICATE_TYPE.register(
        "has_cauldron",
        HasCauldron.Type::new
    );
}
