package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

enum DestroyType {
    FELLING {
        @Override
        void accept(ShockContext context, List<BlockPos> list, DestroyMode mode) {
            Level level = context.level();
            for (BlockPos destroyLayer : list) {
                BlockState blockState = level.getBlockState(destroyLayer);
                if (blockState.isAir()) continue;
                if (isFellingApplicableBlock(blockState)) {
                    BlockPos.breadthFirstTraversal(
                        destroyLayer,
                        TRAVERSE_DEPTH,
                        VISIT_LIMIT,
                        Util::acceptDirections,
                        it -> {
                            if (it.getY() < destroyLayer.getY()) return false;
                            BlockState state = level.getBlockState(it);
                            if (isFellingApplicableBlock(state)) {
                                List<ItemStack> itemStack = mode.apply(state, it, context);
                                level.setBlockAndUpdate(it, Blocks.AIR.defaultBlockState());
                                DestroyType.dropItems(itemStack, it, level);
                                return true;
                            }
                            return false;
                        }
                    );
                }
            }
        }

        private static boolean isFellingApplicableBlock(BlockState blockState) {
            return (blockState.is(BlockTags.LEAVES) && !blockState.getValue(LeavesBlock.PERSISTENT))
                || blockState.is(ModBlockTags.FELLING_APPLICABLE);
        }
    }, HARVESTING {
        @Override
        void accept(ShockContext context, List<BlockPos> list, DestroyMode mode) {
            Level level = context.level();
            for (BlockPos pos : list) {
                BlockPos.MutableBlockPos destroyLayer = pos.mutable();
                BlockState state = level.getBlockState(destroyLayer);
                if (state.isAir()) continue;
                if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                    List<ItemStack> itemStack = mode.apply(state, pos, context);
                    DestroyType.dropItems(itemStack, pos, level);
                    level.setBlockAndUpdate(destroyLayer, cropBlock.getStateForAge(0));
                    continue;
                }
                if (state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == 3) {
                    Block.dropResources(state, level, destroyLayer);
                    level.setBlockAndUpdate(destroyLayer, state.setValue(NetherWartBlock.AGE, 0));
                    continue;
                }
                if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
                    Block.dropResources(state, level, destroyLayer);
                    level.setBlockAndUpdate(destroyLayer, state.setValue(SweetBerryBushBlock.AGE, 1));
                    continue;
                }
                if (state.is(Blocks.COCOA) || state.is(BlockTags.JUNGLE_LOGS)) {
                    BlockPos.breadthFirstTraversal(
                        destroyLayer,
                        TRAVERSE_DEPTH,
                        VISIT_LIMIT,
                        Util::acceptDirections,
                        it -> {
                            if (it.getY() < destroyLayer.getY()) return false;
                            BlockState blockState = level.getBlockState(it);
                            if (blockState.is(Blocks.COCOA) && blockState.getValue(CocoaBlock.AGE) == 2) {
                                List<ItemStack> itemStack = mode.apply(blockState, it, context);
                                level.setBlockAndUpdate(it, blockState.setValue(CocoaBlock.AGE, 0));
                                DestroyType.dropItems(itemStack, it, level);
                                return true;
                            }
                            if (blockState.is(BlockTags.JUNGLE_LOGS)) {
                                return true;
                            }
                            return false;
                        }
                    );
                }
                if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
                    List<ItemStack> itemStack = mode.apply(state, pos, context);
                    DestroyType.dropItems(itemStack, pos, level);
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                if (state.is(Blocks.PITCHER_CROP) && state.getValue(PitcherCropBlock.AGE) == 4) {
                    List<ItemStack> itemStack = new ArrayList<>();
                    if (state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.UPPER) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
                        level.setBlock(pos.below(), Blocks.PITCHER_CROP.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
                        itemStack = mode.apply(
                            state.setValue(PitcherCropBlock.HALF, DoubleBlockHalf.LOWER),
                            pos.below(),
                            context
                        );
                    }
                    if (state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.LOWER) {
                        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
                        level.setBlock(pos, Blocks.PITCHER_CROP.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
                        itemStack = mode.apply(
                            state.setValue(PitcherCropBlock.HALF, DoubleBlockHalf.LOWER),
                            pos,
                            context
                        );
                    }
                    DestroyType.dropItems(itemStack, pos, level);
                }
                if (state.is(Blocks.TORCHFLOWER)) {
                    List<ItemStack> itemStack = mode.apply(state, pos.below(), context);
                    level.setBlockAndUpdate(pos, Blocks.TORCHFLOWER_CROP.defaultBlockState());
                    DestroyType.dropItems(itemStack, pos, level);
                }
                boolean found = false;
                for (int i = 0; i < 2; i++) {
                    if (state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) {
                        found = true;
                        break;
                    }
                    destroyLayer.move(Direction.DOWN);
                    state = level.getBlockState(destroyLayer);
                }
                if (found) {
                    BlockPos.breadthFirstTraversal(
                        destroyLayer,
                        TRAVERSE_DEPTH,
                        VISIT_LIMIT,
                        (it, c) -> c.accept(it.below()),
                        it -> {
                            if (it.getY() > pos.getY()) return false;
                            BlockState blockState = level.getBlockState(it);
                            if (blockState.is(Blocks.CAVE_VINES) || blockState.is(Blocks.CAVE_VINES_PLANT)) {
                                List<ItemStack> itemStack = mode.apply(blockState, it, context);
                                if (blockState.hasProperty(CaveVinesPlantBlock.BERRIES)) {
                                    level.setBlockAndUpdate(it, blockState.setValue(CaveVinesPlantBlock.BERRIES, false));
                                    DestroyType.dropItems(itemStack, it, level);
                                }
                                return true;
                            }
                            return false;
                        }
                    );
                }
            }

        }
    }, CLEANING {
        public static final ItemStack TOOL = Items.SHEARS.getDefaultInstance();

        @Override
        void accept(ShockContext context, List<BlockPos> list, DestroyMode mode) {
            Level level = context.level();
            for (BlockPos pos : list) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.getBlock() instanceof CropBlock || state.is(ModBlockTags.CLEANING_APPLICABLE)) {
                    List<ItemStack> drops;
                    if (state.is(Blocks.SNOW)) {
                        drops = mode.apply(state, pos, context);
                    } else {
                        drops = mode.apply(state, pos, context, TOOL);
                    }

                    DestroyType.dropItems(drops, pos, level);
                    level.destroyBlock(pos, false);
                }
            }
        }
    }, GENERAL {
        @Override
        void accept(ShockContext context, List<BlockPos> list, DestroyMode mode) {
            Level level = context.level();
            for (BlockPos pos : list) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (pos.distSqr(context.centerPos().above()) <= 2) continue;
                if (state.getBlock().defaultDestroyTime() < 0) continue;
                List<ItemStack> drops = mode.apply(state, pos, context);
                DestroyType.dropItems(drops, pos, level);
                level.destroyBlock(pos, false);
            }
        }
    };

    public static final int TRAVERSE_DEPTH = 64;
    public static final int VISIT_LIMIT = 1024;

    abstract void accept(ShockContext context, List<BlockPos> list, DestroyMode mode);

    private static void dropItems(List<ItemStack> itemStacks, BlockPos pos, Level level) {
        for (ItemStack itemStack : itemStacks) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
            level.addFreshEntity(itemEntity);
        }
    }
}