package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;

public class ConcreteRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        for (Map.Entry<Color, BlockEntry<CementCauldronBlock>> entry : ModBlocks.CEMENT_CAULDRONS.entrySet()) {
            Color color = entry.getKey();
            CementCauldronBlock cauldronBlock = entry.getValue().get();
            Item concrete = BuiltInRegistries.ITEM.get(
                ResourceLocation.withDefaultNamespace("%s_concrete".formatted(color.getSerializedName()))
            );
            Item reinforcedConcrete = BuiltInRegistries.ITEM.get(
                AnvilCraft.of("reinforced_concrete_%s".formatted(color.getSerializedName()))
            );
            BulgingRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 4)
                .requires(Tags.Items.SANDS, 4)
                .result(concrete, 16)
                .save(
                    provider,
                    AnvilCraft.of("concrete/minecraft_%s_concrete".formatted(color.getSerializedName()))
                );
            BulgingRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 2)
                .requires(Tags.Items.SANDS, 2)
                .requires(Items.IRON_BARS, 8)
                .result(reinforcedConcrete, 16)
                .save(
                    provider,
                    AnvilCraft.of("concrete/anvilcraft_reinforced_concrete_%s".formatted(color.getSerializedName()))
                );
        }
    }
}
