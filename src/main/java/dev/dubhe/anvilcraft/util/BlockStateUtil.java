package dev.dubhe.anvilcraft.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.mixin.accessor.CropBlockAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.GrowingPlantAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;

/**
 * 方块状态注入
 */
public class BlockStateUtil {
    /**
     * 硬编码一些通过Block#asItem方法获取不到的物品。。。。
     * 这些物品通常可以通过Block#getCloneItemStack方法获取到，但是需要LevelReader实例
     * 为了让获取物品在没有level上下文的情况下也能运作，此处硬编码部分特殊方块
     */
    public static final Map<Block, ItemStack> HARDCODED_SPECIAL_AS_ITEM = ImmutableMap.<Block, ItemStack>builder()
        .put(Blocks.ATTACHED_MELON_STEM, Items.MELON_SEEDS.getDefaultInstance())
        .put(Blocks.ATTACHED_PUMPKIN_STEM, Items.PUMPKIN_SEEDS.getDefaultInstance())
        .put(Blocks.BAMBOO_SAPLING, Items.BAMBOO.getDefaultInstance())
        .put(Blocks.BIG_DRIPLEAF_STEM, Items.BIG_DRIPLEAF.getDefaultInstance())
        .put(Blocks.TALL_GRASS, new ItemStack(Items.SHORT_GRASS, 2))
        .put(Blocks.LARGE_FERN, new ItemStack(Items.FERN, 2))
        .put(Blocks.PISTON_HEAD, ItemStack.EMPTY)
        .build();

    public static final Set<IntegerProperty> COUNT_PROPERTIES = ImmutableSet.of(
        BlockStateProperties.LAYERS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.CANDLES,
        BlockStateProperties.FLOWER_AMOUNT
    );

    /**
     * 判定一个方块是否像苔藓、发光地衣、幽匿脉络一样，可以在一个方块内放置多个面，
     * 每个面消耗一个物品。
     *
     * @param block 需要判定的方块
     * @return 该方块是否是“多面类”方块
     * @apiNote 注：通过这个方法判定的方块不一定每个面都能放，
     * 本方法只表明放置该方块所需的物品数量是否与 {@link PipeBlock#PROPERTY_BY_DIRECTION}
     * 中的方块状态有关。
     */
    public static boolean isMultifaceLike(Block block) {
        return block instanceof MultifaceBlock || block instanceof VineBlock;
    }

    /**
     * 对一个炼药锅方块，尝试获取其对应的流体桶。
     *
     * @param cauldron 被判定的炼药锅方块
     * @param state    被判定的方块状态
     * @return 炼药锅方块对应的流体桶
     * @apiNote 暂时只判定满的炼药锅，因为不满的炼药锅不一定有对应物品。<br/>
     * 由于目前的 {@link BlockStateUtil#ingredientsForPlacement(BlockState)} 只打算返回物品列表
     * （同时返回物品列表和流体列表还是太麻烦了，以后再想办法吧）
     */
    private static ItemStack getBucketFromCauldron(AbstractCauldronBlock cauldron, BlockState state) {
        if (cauldron == Blocks.POWDER_SNOW_CAULDRON) {
            return cauldron.isFull(state) ? Items.POWDER_SNOW_BUCKET.getDefaultInstance() : ItemStack.EMPTY;
        }
        return Optional.of(cauldron)
            .filter(c -> c.isFull(state))
            .map(CauldronFluidContent::getForBlock)
            .map(c -> c.fluid)
            .map(Fluid::getBucket)
            .map(Item::getDefaultInstance)
            .orElse(ItemStack.EMPTY);
    }

    /**
     * 对某个方块状态，获取用于摆放它的物品列表。供多方快合成的JEI显示使用。<br/>
     * <b>不考虑</b>方块实体。<s>要是考虑的话那我真得累死</s><br/>
     * 硬编码了原版和本模组的各种各样的特殊情形。
     *
     * @param state 要摆放的方块状态
     * @return 用于摆放的物品列表
     */
    public static List<ItemStack> ingredientsForPlacement(BlockState state) {
        Block block = state.getBlock();
        ItemStack baseItem = switch (block) {
            case CropBlock crop -> ((CropBlockAccessor) crop).invoker$getBaseSeedId().asItem().getDefaultInstance();
            case FlowerPotBlock ignored -> Items.FLOWER_POT.getDefaultInstance();
            case GrowingPlantBodyBlock plantHead -> ((GrowingPlantAccessor) plantHead).invoker$getHeadBlock()
                .asItem().getDefaultInstance();
            case CandleCakeBlock ignored -> Items.CAKE.getDefaultInstance();
            default -> HARDCODED_SPECIAL_AS_ITEM.getOrDefault(block, block.asItem().getDefaultInstance());
        };
        if (state.hasProperty(DOUBLE_BLOCK_HALF) && state.getValue(DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            baseItem = ItemStack.EMPTY;
        } else if (state.hasProperty(BED_PART) && state.getValue(BED_PART) != BedPart.HEAD) {
            baseItem = ItemStack.EMPTY;
        } else if (block instanceof AbstractMultiPartBlock<?> multiplePartBlock && !multiplePartBlock.isMainPart(state)) {
            baseItem = ItemStack.EMPTY;
        } else if (isMultifaceLike(block)) {
            long faceCount = PipeBlock.PROPERTY_BY_DIRECTION.values().stream()
                .filter(state::hasProperty)
                .filter(state::getValue)
                .count();
            baseItem.setCount((int) faceCount);
        } else if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            baseItem.setCount(2);
        } else {
            ItemStack finalBaseItem = baseItem;
            state.getProperties().stream()
                .filter(IntegerProperty.class::isInstance)
                .map(IntegerProperty.class::cast)
                .filter(COUNT_PROPERTIES::contains)
                .findFirst()
                .ifPresent(p -> finalBaseItem.setCount(state.getValue(p)));
        }
        ItemStack additionalItem = switch (block) {
            case CandleCakeBlock cake -> cake.candleBlock.asItem().getDefaultInstance();
            case FlowerPotBlock pot -> pot.getPotted().asItem().getDefaultInstance();
            case AbstractCauldronBlock cauldron -> getBucketFromCauldron(cauldron, state);
            default -> {
                FluidState fluidState = state.getFluidState();
                if (fluidState.isSource()) {
                    yield fluidState.getType().getBucket().getDefaultInstance();
                } else yield ItemStack.EMPTY;
            }
        };
        if (baseItem.isEmpty() && additionalItem.isEmpty()) return List.of();
        if (additionalItem.isEmpty()) return List.of(baseItem);
        if (baseItem.isEmpty()) return List.of(additionalItem);
        return List.of(baseItem, additionalItem);
    }

    public static class BlockHolderLookup implements HolderLookup<Block>, HolderOwner<Block> {
        @Override
        public @NotNull Stream<Holder.Reference<Block>> listElements() {
            return BuiltInRegistries.BLOCK.stream()
                .map(BuiltInRegistries.BLOCK::getResourceKey)
                .filter(Optional::isPresent)
                .map(key -> BuiltInRegistries.BLOCK.getHolderOrThrow(key.get()));
        }

        @Override
        public @NotNull Stream<HolderSet.Named<Block>> listTags() {
            return BuiltInRegistries.BLOCK.getTags().map(Pair::getSecond);
        }

        @Override
        public @NotNull Optional<Holder.Reference<Block>> get(@NotNull ResourceKey<Block> resourceKey) {
            return Optional.of(BuiltInRegistries.BLOCK.getHolderOrThrow(resourceKey));
        }

        @Override
        public @NotNull Optional<HolderSet.Named<Block>> get(@NotNull TagKey<Block> tagKey) {
            return BuiltInRegistries.BLOCK.getTag(tagKey);
        }
    }
}
