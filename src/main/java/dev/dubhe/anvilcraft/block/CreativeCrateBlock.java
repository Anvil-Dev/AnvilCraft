package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.item.InfinityItemStackHandler;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CreativeCrateBlock extends BaseEntityBlock implements IHammerRemovable {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CreativeCrateBlock::new);
    }

    public CreativeCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CreativeCrateBlockEntity creativeCrateBlockEntity) {
            InfinityItemStackHandler itemStackHandler = creativeCrateBlockEntity.getItemStackHandler();
            if (!itemStackHandler.getStackInSlot(0).isEmpty()) {
                if (!player.addItem(itemStackHandler.getStackInSlot(0).copyWithCount(1))) {
                    popResource(level, BlockPos.containing(player.position()), itemStackHandler.getStackInSlot(0).copyWithCount(1));
                }
            }
            itemStackHandler.setStackInSlot(0, stack.copyWithCount(stack.getMaxStackSize()));
            stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.CREATIVE_CRATE.create(blockPos, blockState);
    }
}
