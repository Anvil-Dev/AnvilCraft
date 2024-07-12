package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.data.recipe.RecipeBlock;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
import dev.dubhe.anvilcraft.data.recipe.RecipeResult;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MultiblockCraftingRecipeLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化粉碎配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        MultiblockCraftingRecipeLoader.provider = provider;
        multiblockCrafting(
                new HashMap<>() {{
                    put('A', RecipeBlock.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get()));
                    put('B', RecipeBlock.of(ModBlocks.HEAVY_IRON_COLUMN.get()));
                    put('C', RecipeBlock.of(ModBlocks.HEAVY_IRON_PLATE.get()));
                    put('D', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_BLOCK.get()));
                }},
                new String[]{
                    "AAA",
                    "AAA",
                    "AAA",

                    "CCC",
                    "CBC",
                    "CCC",

                    "DDD",
                    "DDD",
                    "DDD",
                },
                RecipeResult.of(RecipeItem.of(ModBlocks.GIANT_ANVIL))
        );
        multiblockCrafting(
                new HashMap<>() {{
                    put('A', RecipeBlock.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get()));
                    put('B', RecipeBlock.of(ModBlocks.HEAVY_IRON_BEAM.get(),
                            Map.entry(HeavyIronBeamBlock.AXIS, Direction.Axis.Z)));
                    put('C', RecipeBlock.of(ModBlocks.HEAVY_IRON_BEAM.get(),
                            Map.entry(HeavyIronBeamBlock.AXIS, Direction.Axis.X)));
                    put('D', RecipeBlock.of(ModBlocks.HEAVY_IRON_COLUMN.get()));
                    put('E', RecipeBlock.of(ModBlocks.HEAVY_IRON_PLATE.get()));
                    put('F', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                            Map.entry(StairBlock.FACING, Direction.SOUTH),
                            Map.entry(StairBlock.HALF, Half.BOTTOM)));
                    put('G', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                            Map.entry(StairBlock.FACING, Direction.NORTH),
                            Map.entry(StairBlock.HALF, Half.BOTTOM)));
                    put('H', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                            Map.entry(StairBlock.FACING, Direction.EAST),
                            Map.entry(StairBlock.HALF, Half.BOTTOM)));
                    put('I', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                            Map.entry(StairBlock.FACING, Direction.WEST),
                            Map.entry(StairBlock.HALF, Half.BOTTOM)));
                    put('J', RecipeBlock.of(ModBlocks.CUT_HEAVY_IRON_SLAB.get(),
                            Map.entry(SlabBlock.TYPE, SlabType.BOTTOM)));
                }},
                new String[]{
                    "ABA",
                    "CAC",
                    "ABA",

                    "EEE",
                    "EDE",
                    "EEE",

                    "JGJ",
                    "HDI",
                    "JFJ",
                },
                RecipeResult.of(RecipeItem.of(ModBlocks.GIANT_ANVIL))
        );
        multiblockCrafting(
                new HashMap<>() {{
                    put('A', RecipeBlock.of(Blocks.SPONGE));
                    put('B', RecipeBlock.of(ModBlocks.VOID_MATTER_BLOCK.get()));
                }},
                new String[]{
                    "AAA",
                    "A A",
                    "AAA",

                    "A A",
                    " B ",
                    "A A",

                    "AAA",
                    "A A",
                    "AAA",
                },
                RecipeResult.of(RecipeItem.of(ModBlocks.MENGER_SPONGE))
        );
    }


    /**
     * 大铁砧多方块合成配方
     * maps传参示例
     *
     * <p>new String[]{
     *
     * <p>"AAA",
     *
     * <p>"AAA",
     *
     * <p>"AAA",
     *
     * <p><p>"AAA",
     *
     * <p>"AAA",
     *
     * <p>"AAA",
     *
     * <p><p>"AAA",
     *
     * <p>"AAA",
     *
     * <p>"AAA",
     *
     * <p>}
     *
     * @param mappingTable  方块与字母对照表
     * @param maps          每一行的配方, 要求数量必须为单行配方字数的平方
     * @param recipeResults 配方的输出
     */
    public static void multiblockCrafting(
            Map<Character, RecipeBlock> mappingTable,
            String[] maps,
            RecipeResult... recipeResults
    ) {
        if (Arrays.stream(maps).findFirst().isEmpty()) return;
        int size = Arrays.stream(maps).findFirst().get().length();
        if (maps.length != size * size) return;
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.MULTIBLOCK_CRAFTING);
        if (Arrays.stream(recipeResults).findFirst().isPresent())
            builder = builder.icon(Arrays.stream(recipeResults).findFirst().get().icon());
        for (int x = -(size - 1) / 2; x <= (size - 1) / 2; x++) {
            for (int z = -(size - 1) / 2; z <= (size - 1) / 2; z++) {
                builder = builder.hasBlock(new Vec3(x, -1, z), Blocks.CRAFTING_TABLE);
            }
        }
        StringBuilder path = new StringBuilder("multiblock_crafting/");
        path.append(Arrays.stream(recipeResults).findFirst().get().getKey());
        path.append("_from_");
        int index = 0;
        for (RecipeBlock recipeBlock : mappingTable.values()) {
            if (index >= 6) {
                break;
            }
            path.append(recipeBlock.getKey());
            path.append("_");
            index++;
        }
        mappingTable.put(' ', RecipeBlock.of(Blocks.AIR));
        index = 0;
        for (String map : maps) {
            if (map.length() != size) return;
            for (int i = 0; i < map.length(); i++) {
                char ch = map.charAt(i);
                if (!mappingTable.containsKey(ch)) return;
                builder = builder.hasBlockIngredient(new Vec3(
                                index % 3 - (int) ((double) (size - 1) / 2),
                                (int) (-((double) index / 9) - 2),
                                -((int) (((double) index % 9) / 3) - (int) ((double) (size - 1) / 2))
                        ),
                        mappingTable.get(ch)
                );
                index++;
            }
        }
        for (RecipeResult recipeResult : recipeResults) {
            if (recipeResult.isItem()) {
                builder = builder.spawnItem(recipeResult.offset().add(0, -3, 0), recipeResult.recipeItem());
            } else {
                builder = builder.setBlock(recipeResult.offset().add(0, -3, 0), recipeResult.recipeBlock());
            }
        }
        builder.save(provider, AnvilCraft.of(path.substring(0, path.length() - 1)));
    }
}