package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

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
        initConcrete(provider);
        initCementStaining(provider);
        initCementDyeing(provider);
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
            SolidLiquidRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 4)
                .requires(Tags.Items.SANDS, 4)
                .result(concrete, 16)
                .save(provider, AnvilCraft.of("solid_liquid/concrete/minecraft_%s_concrete".formatted(color.getSerializedName())));
            SolidLiquidRecipe.builder()
                .cauldron(cauldronBlock)
                .requires(Tags.Items.GRAVELS, 2)
                .requires(Tags.Items.SANDS, 2)
                .requires(Items.IRON_BARS, 8)
                .result(reinforcedConcrete, 16)
                .save(provider, AnvilCraft.of("solid_liquid/concrete/anvilcraft_reinforced_concrete_%s"
                    .formatted(color.getSerializedName())));
        }
    }

    /**
     * 生成水泥染色配方（铁砧加工），每种染料对应将任意颜色水泥炼药锅/鱼缸中的水泥染成对应颜色
     */
    private static void initCementStaining(RegistrumRecipeProvider provider) {
        ResourceLocation cementTag = ResourceLocation.fromNamespaceAndPath("c", "cement");
        for (Color color : Color.values()) {
            ResourceLocation targetCement = AnvilCraft.of("%s_cement".formatted(color.getSerializedName()));
            SolidLiquidRecipe.builder()
                .fluidTag(cementTag)
                .transform(targetCement)
                .requires(color.dyeItem())
                .save(provider, AnvilCraft.of("solid_liquid/cement_staining/%s".formatted(color.getSerializedName())));
        }
    }

    /**
     * Uses colored cement as a reusable dye bath for common color-variant item families.
     */
    private static void initCementDyeing(RegistrumRecipeProvider provider) {
        for (Color color : Color.values()) {
            String colorName = color.getSerializedName();
            CementCauldronBlock cauldron = ModBlocks.CEMENT_CAULDRONS.get(color).get();
            for (DyeableFamily family : DYEABLE_FAMILIES) {
                Item result = BuiltInRegistries.ITEM.get(
                    ResourceLocation.withDefaultNamespace("%s_%s".formatted(colorName, family.resultSuffix()))
                );
                SolidLiquidRecipe.builder()
                .cauldron(cauldron)
                .requires(family.ingredients())
                .result(result)
                .save(provider, AnvilCraft.of("solid_liquid/cement_dyeing/%s/%s".formatted(colorName, family.recipeName()))
                );
            }
        }
    }

    private record DyeableFamily(String recipeName, TagKey<Item> ingredients, String resultSuffix) {
    }
}
