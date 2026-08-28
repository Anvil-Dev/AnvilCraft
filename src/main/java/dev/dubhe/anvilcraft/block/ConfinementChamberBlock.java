package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ConfinementChamberBlock extends BaseEntityBlock implements IHammerRemovable, ITranscendiumBlock {
    public ConfinementChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ConfinementChamberBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ConfinementChamberBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Unimplemented for current version
    /*@Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ConfinementChamberBlockEntity confinementChamberBlockEntity))
            return ItemInteractionResult.FAIL;
        ItemStack itemStack = confinementChamberBlockEntity.getItemHandler().getStackInSlot(0);
        ItemStack handItemStack = player.getItemInHand(hand);
        if (itemStack.is(handItemStack.getItem())) return ItemInteractionResult.FAIL;
        player.setItemInHand(hand, itemStack.copy());
        confinementChamberBlockEntity.getItemHandler().setStackInSlot(0, handItemStack.copy());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }*/

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ConfinementChamberBlockEntity confinementChamberBlockEntity) {
            if (!level.isClientSide && player.isCreative() && !confinementChamberBlockEntity.getItemHandler().getStackInSlot(0).isEmpty()) {
                ItemStack itemstack = new ItemStack(ModBlocks.CONFINEMENT_CHAMBER.asItem());
                itemstack.applyComponents(blockentity.collectComponents());
                ItemEntity itementity = new ItemEntity(
                    level, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemstack
                );
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
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
        if (!level.isClientSide) {
            if (stack.is(ModItems.CHARGED_NEUTRONIUM_INGOT)) {
                level.setBlockAndUpdate(pos, ModBlocks.CONFINED_NEUTRONIUM_INGOT_BLOCK.getDefaultState());
                stack.consume(1, player);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
