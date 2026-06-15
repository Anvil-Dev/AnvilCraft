package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class CreativeFluidTankBlock extends BaseEntityBlock implements IHammerRemovable {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CreativeFluidTankBlock::new);
    }

    public CreativeFluidTankBlock(Properties properties) {
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
        IFluidHandlerItem bucket = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (bucket != null) {
            if (level.isClientSide()) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            FluidStack fluidInBucket = bucket.getFluidInTank(0);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CreativeFluidTankBlockEntity creativeFluidTankBlockEntity) {
                IFluidHandler tank = creativeFluidTankBlockEntity.getFluidHandler();
                if (tank instanceof FluidTank fluidTank) {
                    if (fluidInBucket.isEmpty() && !fluidTank.isEmpty()) {
                        SoundEvent sound = fluidTank.getFluid().getFluidType().getSound(SoundActions.BUCKET_FILL);
                        if (player.isCreative()) {
                            fluidTank.setFluid(FluidStack.EMPTY);
                            creativeFluidTankBlockEntity.setChanged();
                            level.sendBlockUpdated(pos, state, state, 3);
                        } else {
                            int space = bucket.getTankCapacity(0) - fluidInBucket.getAmount();
                            if (space > 0) {
                                FluidStack drained = fluidTank.drain(space, IFluidHandler.FluidAction.SIMULATE);
                                if (!drained.isEmpty()) {
                                    int fillable = bucket.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                                    if (fillable > 0) {
                                        FluidStack toFill = fluidTank.drain(fillable, IFluidHandler.FluidAction.EXECUTE);
                                        bucket.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                                        player.setItemInHand(hand, bucket.getContainer());
                                        creativeFluidTankBlockEntity.setChanged();
                                    }
                                }
                            }
                        }
                        if (sound != null) {
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS);
                        }
                    } else {
                        fluidTank.setFluid(fluidInBucket.copyWithAmount(Integer.MAX_VALUE));
                        creativeFluidTankBlockEntity.setChanged();
                        level.sendBlockUpdated(pos, state, state, 3);
                        if (!player.isCreative()) {
                            bucket.drain(fluidInBucket.getAmount(), IFluidHandler.FluidAction.EXECUTE);
                            player.setItemInHand(hand, bucket.getContainer());
                        }
                        SoundEvent sound = fluidInBucket.getFluidType().getSound(SoundActions.BUCKET_EMPTY);
                        if (sound != null) {
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.CREATIVE_FLUID_TANK.create(blockPos, blockState);
    }
}
