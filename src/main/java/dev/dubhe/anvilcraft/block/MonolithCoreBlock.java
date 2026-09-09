package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MonolithCoreBlock extends MonolithBlock implements EntityBlock {
    public MonolithCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MONOLITH_CORE.create(pos, state);
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
        if (!MonolithCoreBlockEntity.acceptsOffering(stack, false)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (!(level.getBlockEntity(pos) instanceof MonolithCoreBlockEntity core) || !core.beginOffering(stack)) {
                return ItemInteractionResult.CONSUME;
            }
            stack.consume(1, player);
            ItemStack book = ModItems.GUIDE_BOOK.asStack();
            if (!player.addItem(book)) player.drop(book, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
