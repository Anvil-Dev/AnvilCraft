package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;

public class ConcreteRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        initConcrete(provider);
        initCementStaining(provider);
    }

    private static void initConcrete(RegistrumRecipeProvider provider) {
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

    /**
     * 生成水泥染色配方（铁砧加工），每种染料对应将任意颜色水泥炼药锅/鱼缸中的水泥染成对应颜色
     */
    private static void initCementStaining(RegistrumRecipeProvider provider) {
        ResourceLocation cementTag = ResourceLocation.fromNamespaceAndPath("c", "cement");
        for (Color color : Color.values()) {
            ResourceLocation targetCement = AnvilCraft.of("%s_cement".formatted(color.getSerializedName()));
            BulgingRecipe.builder()
                .fluidTag(cementTag)
                .transform(targetCement)
                .requires(color.dyeItem())
                .save(provider, AnvilCraft.of("cement_staining/%s".formatted(color.getSerializedName())));
        }
    }
}
