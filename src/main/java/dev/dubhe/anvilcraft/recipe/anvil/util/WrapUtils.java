package dev.dubhe.anvilcraft.recipe.anvil.util;

import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlockIngredient;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 包装工具类
 * <p>
 * 提供各种配方相关对象的创建和转换工具方法
 * </p>
 */
public class WrapUtils {
    /**
     * 根据方块状态谓词列表创建HasBlock谓词列表
     *
     * @param results 方块状态谓词列表
     * @return HasBlock谓词列表
     */
    public static @NotNull List<IRecipePredicate<?>> getPredicates(
        @NotNull List<BlockStatePredicate> results
    ) {
        List<IRecipePredicate<?>> predicates = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            BlockStatePredicate result = results.get(i);
            predicates.add(new HasBlock(new Vec3(0, -i - 1, 0), result));
        }
        return predicates;
    }

    /**
     * 根据方块状态谓词创建HasBlockIngredient谓词
     *
     * @param block 方块状态谓词
     * @return HasBlockIngredient谓词
     */
    public static @NotNull IRecipePredicate<?> getIngredientPredicate(
        @NotNull BlockStatePredicate block
    ) {
        return new HasBlockIngredient(new Vec3(0, -1, 0), block);
    }

    /**
     * 根据方块状态谓词创建HasBlockIngredient谓词列表（不可变）
     *
     * @param block 方块状态谓词
     * @return HasBlockIngredient谓词列表
     */
    public static @NotNull @Unmodifiable List<IRecipePredicate<?>> getIngredientPredicates(
        @NotNull BlockStatePredicate block
    ) {
        return List.of(getIngredientPredicate(block));
    }

    /**
     * 根据方块状态谓词列表创建HasBlockIngredient谓词列表
     *
     * @param results 方块状态谓词列表
     * @return HasBlockIngredient谓词列表
     */
    public static @NotNull List<IRecipePredicate<?>> getIngredientPredicates(
        @NotNull List<BlockStatePredicate> results
    ) {
        List<IRecipePredicate<?>> predicates = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            BlockStatePredicate result = results.get(i);
            predicates.add(new HasBlockIngredient(new Vec3(0, -i - 1, 0), result));
        }
        return predicates;
    }

    /**
     * 根据ChanceBlockState和Y轴偏移创建结果列表（不可变）
     *
     * @param result  ChanceBlockState
     * @param yOffset Y轴偏移
     * @return 结果列表
     */
    public static @NotNull @Unmodifiable List<IRecipeOutcome<?>> getOutcomes(
        @NotNull ChanceBlockState result,
        int yOffset
    ) {
        return List.of(result.toSetBlock(new Vec3(0, yOffset, 0)));
    }

    /**
     * 根据ChanceBlockState创建结果列表（不可变）
     *
     * @param result ChanceBlockState
     * @return 结果列表
     */
    public static @NotNull @Unmodifiable List<IRecipeOutcome<?>> getOutcomes(
        @NotNull ChanceBlockState result
    ) {
        return WrapUtils.getOutcomes(result, -1);
    }

    /**
     * 根据ChanceBlockState列表创建结果列表
     *
     * @param results ChanceBlockState列表
     * @return 结果列表
     */
    public static @NotNull List<IRecipeOutcome<?>> getOutcomes(
        @NotNull List<ChanceBlockState> results
    ) {
        List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ChanceBlockState result = results.get(i);
            outcomes.add(result.toSetBlock(new Vec3(0, -i - 1, 0)));
        }
        return outcomes;
    }

    /**
     * 根据ChanceBlockState获取物品
     *
     * @param result ChanceBlockState
     * @return 物品
     */
    public static @NotNull Item getItem(@NotNull ChanceBlockState result) {
        BlockState state = result.getState();
        if (state.isEmpty() || state.isAir()) return Items.ANVIL;
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) item = Items.ANVIL;
        return item;
    }

    /**
     * 根据ChanceBlockState列表获取物品
     *
     * @param results ChanceBlockState列表
     * @return 物品
     */
    public static @NotNull Item getItem(@NotNull List<ChanceBlockState> results) {
        if (results.isEmpty()) return Items.ANVIL;
        return WrapUtils.getItem(results.getFirst());
    }

    /**
     * 根据ChanceBlockState获取物品堆栈
     *
     * @param result ChanceBlockState
     * @return 物品堆栈
     */
    public static @NotNull ItemStack getItemStack(@NotNull ChanceBlockState result) {
        return WrapUtils.getItem(result).getDefaultInstance();
    }

    /**
     * 根据ChanceBlockState列表获取物品堆栈
     *
     * @param results ChanceBlockState列表
     * @return 物品堆栈
     */
    public static @NotNull ItemStack getItemStack(@NotNull List<ChanceBlockState> results) {
        if (results.isEmpty()) return Items.ANVIL.getDefaultInstance();
        return WrapUtils.getItem(results.getFirst()).getDefaultInstance();
    }

    /**
     * 将炼药锅方块转换为流体ID
     *
     * @param cauldron 炼药锅方块
     * @return 流体ID
     */
    public static @NotNull ResourceLocation cauldron2Fluid(Block cauldron) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(cauldron);
        String namespace = key.getNamespace();
        String path = key.getPath();
        if (path.endsWith("_cauldron")) path = path.substring(0, path.length() - 9);
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}