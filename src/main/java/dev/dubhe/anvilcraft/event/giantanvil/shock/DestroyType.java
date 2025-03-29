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
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

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
            return blockState.is(BlockTags.LOGS)
                || (blockState.is(BlockTags.LEAVES) && !blockState.getValue(LeavesBlock.PERSISTENT))
                || blockState.is(Blocks.MANGROVE_ROOTS)
                || blockState.is(ModBlockTags.MUSHROOM_BLOCK)
                || blockState.is(BlockTags.WART_BLOCKS)
                || blockState.is(Blocks.SHROOMLIGHT)
                || blockState.is(Blocks.MUSHROOM_STEM)
                || blockState.is(Blocks.SUGAR_CANE)
                || blockState.is(Blocks.BAMBOO_BLOCK)
                || blockState.is(Blocks.CHORUS_PLANT)
                || blockState.is(Blocks.CHORUS_FLOWER)
                || blockState.is(BlockTags.BEEHIVES);
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
                    Block.dropResources(state, level, destroyLayer);
                    level.setBlockAndUpdate(destroyLayer, cropBlock.getStateForAge(0));
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
                                level.setBlockAndUpdate(it, Blocks.AIR.defaultBlockState());
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
                            if (it.getY() < pos.getY()) return false;
                            BlockState blockState = level.getBlockState(it);
                            if (blockState.is(Blocks.CAVE_VINES) || blockState.is(Blocks.CAVE_VINES_PLANT)) {
                                List<ItemStack> itemStack = mode.apply(blockState, it, context);
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
    }, CLEANING {
        public static final ItemStack TOOL = Items.SHEARS.getDefaultInstance();

        @Override
        void accept(ShockContext context, List<BlockPos> list, DestroyMode mode) {
            Level level = context.level();
            for (BlockPos pos : list) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(Blocks.DEAD_BUSH)
                    || state.getBlock() instanceof CropBlock
                ) {
                    List<ItemStack> drops = mode.apply(state, pos, context, TOOL);
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
                if (state.is(Blocks.BEDROCK)
                    || state.is(Blocks.REINFORCED_DEEPSLATE)
                    || state.is(Blocks.END_GATEWAY)
                    || state.is(Blocks.END_PORTAL)
                    || state.is(Blocks.END_PORTAL_FRAME)
                ) continue;
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