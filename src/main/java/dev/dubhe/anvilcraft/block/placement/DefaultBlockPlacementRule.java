package dev.dubhe.anvilcraft.block.placement;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import dev.dubhe.anvilcraft.mixin.accessor.CropBlockAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.GrowingPlantAccessor;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.CauldronFluidContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Built-in fallback for vanilla placement items and state-dependent counts.
 */
public final class DefaultBlockPlacementRule implements IBlockPlacementRule {
    public static final DefaultBlockPlacementRule INSTANCE = new DefaultBlockPlacementRule();
    private static final Map<Block, ItemStack> SPECIAL_PLACEMENT_ITEMS = ImmutableMap.<Block, ItemStack>builder()
        .put(Blocks.ATTACHED_MELON_STEM, Items.MELON_SEEDS.getDefaultInstance())
        .put(Blocks.ATTACHED_PUMPKIN_STEM, Items.PUMPKIN_SEEDS.getDefaultInstance())
        .put(Blocks.BAMBOO_SAPLING, Items.BAMBOO.getDefaultInstance())
        .put(Blocks.BIG_DRIPLEAF_STEM, Items.BIG_DRIPLEAF.getDefaultInstance())
        .put(Blocks.TALL_GRASS, new ItemStack(Items.SHORT_GRASS, 2))
        .put(Blocks.LARGE_FERN, new ItemStack(Items.FERN, 2))
        .put(Blocks.PISTON_HEAD, ItemStack.EMPTY)
        .build();
    private static final Set<IntegerProperty> ITEM_COUNT_PROPERTIES = ImmutableSet.of(
        BlockStateProperties.LAYERS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.CANDLES,
        BlockStateProperties.FLOWER_AMOUNT
    );

    private DefaultBlockPlacementRule() {
    }

    @Override
    public boolean matches(BlockState state) {
        return true;
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        ItemStack baseItem = this.getBaseItem(state);
        ItemStack additionalItem = this.getAdditionalItem(state);
        List<PlacementItem> items = new ArrayList<>(2);
        if (!baseItem.isEmpty()) {
            items.add(new PlacementItem(baseItem.getItem(), baseItem.getCount()));
        }
        if (!additionalItem.isEmpty()) {
            items.add(new PlacementItem(additionalItem.getItem(), additionalItem.getCount()));
        }
        return items;
    }

    private ItemStack getBaseItem(BlockState state) {
        Block block = state.getBlock();
        ItemStack baseItem = switch (block) {
            case CropBlock crop -> ((CropBlockAccessor) crop).invokeGetBaseSeedId().asItem().getDefaultInstance();
            case FlowerPotBlock ignored -> Items.FLOWER_POT.getDefaultInstance();
            case GrowingPlantBodyBlock plantHead -> ((GrowingPlantAccessor) plantHead).invokeGetHeadBlock()
                .asItem().getDefaultInstance();
            case CandleCakeBlock ignored -> Items.CAKE.getDefaultInstance();
            default -> SPECIAL_PLACEMENT_ITEMS.getOrDefault(block, block.asItem().getDefaultInstance()).copy();
        };
        if (BlockPlacementUtil.isSecondaryMultiblockPart(state)) {
            return ItemStack.EMPTY;
        }
        if (BlockPlacementUtil.isMultifaceLike(block)) {
            long faceCount = PipeBlock.PROPERTY_BY_DIRECTION.values().stream()
                .filter(state::hasProperty)
                .filter(state::getValue)
                .count();
            baseItem.setCount((int) faceCount);
        } else if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            baseItem.setCount(2);
        } else {
            state.getProperties().stream()
                .filter(IntegerProperty.class::isInstance)
                .map(IntegerProperty.class::cast)
                .filter(ITEM_COUNT_PROPERTIES::contains)
                .findFirst()
                .ifPresent(property -> baseItem.setCount(state.getValue(property)));
        }
        return baseItem;
    }

    private ItemStack getAdditionalItem(BlockState state) {
        return switch (state.getBlock()) {
            case CandleCakeBlock cake -> cake.candleBlock.asItem().getDefaultInstance();
            case FlowerPotBlock pot -> pot.getPotted().asItem().getDefaultInstance();
            case AbstractCauldronBlock cauldron -> getBucketFromCauldron(cauldron, state);
            default -> getContainedFluidBucket(state);
        };
    }

    private static ItemStack getBucketFromCauldron(AbstractCauldronBlock cauldron, BlockState state) {
        if (cauldron == Blocks.POWDER_SNOW_CAULDRON) {
            return cauldron.isFull(state) ? Items.POWDER_SNOW_BUCKET.getDefaultInstance() : ItemStack.EMPTY;
        }
        return Optional.of(cauldron)
            .filter(block -> block.isFull(state))
            .map(CauldronFluidContent::getForBlock)
            .map(content -> content.fluid)
            .map(Fluid::getBucket)
            .map(Item::getDefaultInstance)
            .orElse(ItemStack.EMPTY);
    }

    private static ItemStack getContainedFluidBucket(BlockState state) {
        FluidState fluidState = state.getFluidState();
        return fluidState.isSource() ? fluidState.getType().getBucket().getDefaultInstance() : ItemStack.EMPTY;
    }
}
