package dev.dubhe.anvilcraft.init.recipe;

import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeOutcomeTypes {
    public static final DeferredRegister<IRecipeOutcome.Type<?>> OUTCOME_TYPE = DeferredRegister.create(
        LibRegistries.OUTCOME_TYPE_REGISTRY,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IRecipeOutcome.Type<?>, DamageAnvil.Type> DAMAGE_ANVIL = OUTCOME_TYPE.register(
        "damage_anvil",
        DamageAnvil.Type::new
    );

    public static final DeferredHolder<IRecipeOutcome.Type<?>, ProduceHeat.Type> PRODUCE_HEAT = OUTCOME_TYPE.register(
        "produce_heat",
        ProduceHeat.Type::new
    );

    public static final DeferredHolder<IRecipeOutcome.Type<?>, RoyalPreferenceOutcome.Type> ROYAL_PREFERENCE = OUTCOME_TYPE.register(
        "royal_preference",
        RoyalPreferenceOutcome.Type::new
    );

    public static final DeferredHolder<IRecipeOutcome.Type<?>, SuperHeatingRecipe.ConsumeFuel.Type>
        CONSUME_BURNING_HEATER_FUEL = OUTCOME_TYPE.register(
        "consume_burning_heater_fuel",
        SuperHeatingRecipe.ConsumeFuel.Type::new
    );
}
