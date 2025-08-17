package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SpawnItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeOutcomeTypes {
    public static final DeferredRegister<IRecipeOutcome.Type<?>> OUTCOME_TYPE = DeferredRegister
        .create(ModRegistries.OUTCOME_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, DamageAnvil.Type> DAMAGE_ANVIL = OUTCOME_TYPE
        .register("damage_anvil", DamageAnvil.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, SpawnItem.Type> SPAWN_ITEM = OUTCOME_TYPE
        .register("spawn_item", SpawnItem.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, SetBlock.Type> SET_BLOCK = OUTCOME_TYPE
        .register("set_block", SetBlock.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, ProduceHeat.Type> PRODUCE_HEAT = OUTCOME_TYPE
        .register("produce_heat", ProduceHeat.Type::new);
}
