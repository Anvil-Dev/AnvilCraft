package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.ConcreteRecipe;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class ConcreteRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ConcreteRecipe.builder()
            .requires(Tags.Items.GRAVELS, 4)
            .requires(Tags.Items.SANDS, 4)
            .result("minecraft:%s_concrete")
            .resultCount(16)
            .save(provider, AnvilCraft.of("concrete/minecraft_concrete"));

        ConcreteRecipe.builder()
            .requires(Tags.Items.GRAVELS, 2)
            .requires(Tags.Items.SANDS, 2)
            .requires(Items.IRON_BARS, 8)
            .result("anvilcraft:reinforced_concrete_%s")
            .resultCount(16)
            .save(provider, AnvilCraft.of("concrete/anvilcraft_reinforced_concrete"));
    }
}
