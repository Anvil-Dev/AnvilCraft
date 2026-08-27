package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 加工台系列方块之间的转化逻辑。
 *
 * <p>上表面用于放入/取出物品；其余面使用转化道具会把冲压平台改装为对应加工台并消耗道具，
 * 手持铁砧锤右键侧面会把三种加工台还原为冲压平台。
 */
public final class UseItemOnBlock {
    private UseItemOnBlock() {
    }

    /**
     * 非上表面右击时处理转化。
     *
     * @return 是否处理了这次交互
     */
    public static ItemInteractionResult tryConvert(
        Level level,
        Player player,
        InteractionHand hand,
        BlockState state,
        BlockPos pos,
        BlockHitResult hitResult
    ) {
        if (hitResult.getDirection() == Direction.UP) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof AnvilHammerItem) {
            return tryHammerConvert(level, player, state, pos);
        }
        return tryItemConvert(level, player, stack, state, pos);
    }

    private static ItemInteractionResult tryItemConvert(
        Level level,
        Player player,
        ItemStack stack,
        BlockState state,
        BlockPos pos
    ) {
        Block target = null;
        if (stack.is(Items.IRON_TRAPDOOR)) {
            target = ModBlocks.UNPACKING_TABLE.get();
        } else if (stack.is(Blocks.SCAFFOLDING.asItem())) {
            target = ModBlocks.SIFTING_TABLE.get();
        } else if (stack.is(Items.GRINDSTONE)) {
            target = ModBlocks.CRUSHING_TABLE.get();
        }
        if (target == null || state.is(target)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide()) {
            ItemStack material = materialFor(state);
            if (!material.isEmpty()) {
                player.getInventory().placeItemBackInInventory(material);
            }
            stack.shrink(1);
            BlockState newState = target.defaultBlockState().setValue(
                BlockStateProperties.WATERLOGGED,
                state.getValue(BlockStateProperties.WATERLOGGED)
            );
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static ItemInteractionResult tryHammerConvert(
        Level level,
        Player player,
        BlockState state,
        BlockPos pos
    ) {
        if (state.is(ModBlocks.STAMPING_PLATFORM.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide()) {
            BlockState stamping = ModBlocks.STAMPING_PLATFORM.get().defaultBlockState().setValue(
                BlockStateProperties.WATERLOGGED,
                state.getValue(BlockStateProperties.WATERLOGGED)
            );
            ItemStack material = materialFor(state);
            if (!material.isEmpty()) {
                player.getInventory().placeItemBackInInventory(material);
            }
            level.setBlock(pos, stamping, Block.UPDATE_ALL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static ItemStack materialFor(BlockState state) {
        if (state.is(ModBlocks.CRUSHING_TABLE.get())) return new ItemStack(Items.GRINDSTONE);
        if (state.is(ModBlocks.SIFTING_TABLE.get())) return new ItemStack(Blocks.SCAFFOLDING.asItem());
        if (state.is(ModBlocks.UNPACKING_TABLE.get())) return new ItemStack(Items.IRON_TRAPDOOR);
        return ItemStack.EMPTY;
    }
}
