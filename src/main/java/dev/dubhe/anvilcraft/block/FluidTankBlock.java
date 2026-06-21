package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

public class FluidTankBlock extends BaseEntityBlock implements HammerRotateBehavior, IHammerRemovable, IController {

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FluidTankBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FLUID_TANK.create(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult).result();
        if (result == InteractionResult.PASS) {
            if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
                if (tank.onPlayerUse(player, hand)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public ResourceLocation getDefinitionId() {
        return ModMultiblockDefinitions.FLUID_TANK.location();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onFormed);
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onUnformed);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof FluidTankBlockEntity be) {
            return be.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        AuxiliaryLightManager manager = level.getAuxLightManager(pos);
        if (manager == null) return 0;
        return manager.getLightAt(pos);
    }
}
