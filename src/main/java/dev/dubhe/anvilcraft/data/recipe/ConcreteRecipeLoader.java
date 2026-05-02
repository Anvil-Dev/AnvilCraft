package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;

public class ConcreteRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        for (Map.Entry<Color, BlockEntry<CementCauldronBlock>> entry : ModBlocks.CEMENT_CAULDRONS.entrySet()) {
            Color color = entry.getKey();
            CementCauldronBlock cauldronBlock = entry.getValue().get();
            Item concrete = BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace("%s_concrete".formatted(color.getSerializedName()))
            );
            Item reinforcedConcrete = BuiltInRegistries.ITEM.getValue(
                AnvilCraft.of("reinforced_concrete_%s".formatted(color.getSerializedName()))
            );
            BulgingRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 4)
                .requires(Tags.Items.SANDS, 4)
                .result(concrete, 16)
                .save(provider, AnvilCraft.of("concrete/minecraft_%s_concrete".formatted(color.getSerializedName())));
            BulgingRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 2)
                .requires(Tags.Items.SANDS, 2)
                .requires(Items.IRON_BARS, 8)
                .result(reinforcedConcrete, 16)
                .save(provider, AnvilCraft.of("concrete/anvilcraft_reinforced_concrete_%s".formatted(color.getSerializedName())));
        }
    }
}
