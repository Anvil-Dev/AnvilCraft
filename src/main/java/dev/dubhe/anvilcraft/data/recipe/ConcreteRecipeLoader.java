package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cauldron.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConcreteRecipeLoader {
    private static final List<DyeableFamily> DYEABLE_FAMILIES = List.of(
        new DyeableFamily("wool", ItemTags.WOOL, "wool"),
        new DyeableFamily("wool_carpet", ItemTags.WOOL_CARPETS, "carpet"),
        new DyeableFamily("bed", ItemTags.BEDS, "bed"),
        new DyeableFamily("candle", ItemTags.CANDLES, "candle"),
        new DyeableFamily("terracotta", ItemTags.TERRACOTTA, "terracotta"),
        new DyeableFamily("glazed_terracotta", Tags.Items.GLAZED_TERRACOTTAS, "glazed_terracotta"),
        new DyeableFamily("stained_glass", Tags.Items.GLASS_BLOCKS_CHEAP, "stained_glass"),
        new DyeableFamily("stained_glass_pane", Tags.Items.GLASS_PANES, "stained_glass_pane"),
        new DyeableFamily("concrete", Tags.Items.CONCRETES, "concrete"),
        new DyeableFamily("concrete_powder", Tags.Items.CONCRETE_POWDERS, "concrete_powder")
    );

    public static void init(RegistrumRecipeProvider provider) {
        ConcreteRecipeLoader.initConcrete(provider);
        ConcreteRecipeLoader.initCementStaining(provider);
        ConcreteRecipeLoader.initCementDyeing(provider);
    }

    private static void initConcrete(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        for (Map.Entry<Color, BlockEntry<CementCauldronBlock>> entry : ModBlocks.CEMENT_CAULDRONS.entrySet()) {
            Color color = entry.getKey();
            CementCauldronBlock cauldronBlock = entry.getValue().get();
            Item concrete = BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace("%s_concrete".formatted(color.getSerializedName()))
            );
            Item reinforcedConcrete = ModBlocks.REINFORCED_CONCRETES.get(color).asItem();
            SolidLiquidRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(items, Tags.Items.GRAVELS, 4)
                .requires(items, Tags.Items.SANDS, 4)
                .result(concrete, 16)
                .save(provider, AnvilCraft.of("solid_liquid/concrete/minecraft_%s_concrete"
                    .formatted(color.getSerializedName())));
            SolidLiquidRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(items, Tags.Items.GRAVELS, 2)
                .requires(items, Tags.Items.SANDS, 2)
                .requires(Items.IRON_BARS, 8)
                .result(reinforcedConcrete, 16)
                .save(provider, AnvilCraft.of("solid_liquid/concrete/anvilcraft_reinforced_concrete_%s"
                    .formatted(color.getSerializedName())));
        }
    }

    private static void initCementStaining(RegistrumRecipeProvider provider) {
        for (Color color : Color.values()) {
            FluidStackPredicate inputCements = FluidStackPredicate.builder()
                .fluid(Arrays.stream(Color.values())
                    .filter(other -> other != color)
                    .map(other -> ModFluids.SOURCE_CEMENTS.get(other).get())
                    .toArray(Fluid[]::new))
                .build();
            Identifier targetCement = AnvilCraft.of("%s_cement".formatted(color.getSerializedName()));
            SolidLiquidRecipe.builder()
                .cauldron(inputCements)
                .consume(1000)
                .transform(BuiltInRegistries.FLUID.getValue(targetCement), 1000)
                .requires(color.dyeItem())
                .maxEfficiency(1)
                .save(provider, AnvilCraft.of("solid_liquid/cement_staining/%s".formatted(color.getSerializedName())));
        }
    }

    /**
     * Uses colored cement as a reusable dye bath for common color-variant item families.
     */
    private static void initCementDyeing(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        for (Color color : Color.values()) {
            String colorName = color.getSerializedName();
            CementCauldronBlock cauldron = ModBlocks.CEMENT_CAULDRONS.get(color).get();
            for (DyeableFamily family : ConcreteRecipeLoader.DYEABLE_FAMILIES) {
                Item result = BuiltInRegistries.ITEM.getValue(
                    Identifier.withDefaultNamespace("%s_%s".formatted(colorName, family.resultSuffix()))
                );
                SolidLiquidRecipe.builder()
                    .cauldron(cauldron)
                    .requires(items, family.ingredients())
                    .result(result)
                    .save(
                        provider,
                        AnvilCraft.of("solid_liquid/cement_dyeing/%s/%s".formatted(colorName, family.recipeName()))
                );
            }
        }
    }

    private record DyeableFamily(String recipeName, TagKey<Item> ingredients, String resultSuffix) {
    }
}
